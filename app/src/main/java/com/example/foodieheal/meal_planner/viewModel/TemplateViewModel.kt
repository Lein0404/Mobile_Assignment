package com.example.foodieheal.meal_planner.viewModel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.MealSlotDTO
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.meal_planner.model.toEntity
import com.example.foodieheal.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek

class TemplateViewModel(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    private val recipeRepository: RecipeRepository,
    currentUserIdFlow: Flow<String?>
) : ViewModel() {

    private companion object {
        private const val TAG = "TemplateViewModel"
    }

    private val planId: String? = savedStateHandle.get<String>("planId")

    var currentUserId: String? = null
        private set

    init {
        Log.d(TAG, "Initialized TemplateViewModel | SavedStateHandle planId: $planId")
        viewModelScope.launch {
            currentUserIdFlow.collect { id ->
                currentUserId = id
            }
        }
    }

    private val refreshTrigger = MutableStateFlow(0)

    fun refreshPlans() {
        refreshTrigger.value++
    }

    /**
     * Exposes a hot UI StateFlow stream containing completely populated meal cards for ALL users.
     * Automatically re-fetches whenever refreshTrigger is emitted.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allWeeklyPlans: StateFlow<List<WeeklyPlan>> = refreshTrigger
        .flatMapLatest {
            planRepository.observeAllPlans()
        }
        .mapLatest { entityList ->
            entityList.map { entity ->
                WeeklyPlan(
                    planName = entity.planName,
                    planId = entity.planId,
                    userId = entity.userId,
                    category = entity.category,
                    dailyPlans = hydrateDailyPlans(entity.dailyPlans),
                    public = entity.public
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Exposes a hot UI StateFlow filtered specifically for the logged-in user.
     */
    val userWeeklyPlans: StateFlow<List<WeeklyPlan>> = combine(
        allWeeklyPlans,
        currentUserIdFlow
    ) { plans, currentUserId ->
        if (currentUserId.isNullOrBlank()) {
            emptyList()
        } else {
            val userPlans = plans.filter { it.userId == currentUserId }
            userPlans
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Exposes public community plans created by other users.
     */
    val publicCommunityPlans: StateFlow<List<WeeklyPlan>> = combine(
        allWeeklyPlans,
        currentUserIdFlow
    ) { plans, currentUserId ->
        val communityPlans = plans.filter { it.public && it.userId != currentUserId }
        communityPlans
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Deletes a weekly plan template and triggers a refresh.
     */
    fun deleteWeeklyPlan(
        planId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                planRepository.deletePlan(planId)

                // 🌟 Forces allWeeklyPlans to re-fetch and re-hydrate
                refreshPlans()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to delete plan")
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPlan: StateFlow<WeeklyPlan?> = allWeeklyPlans
        .mapLatest { plans ->
            val found = if (planId != null) plans.find { it.planId == planId } else null
            found
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Removes a recipe from all slots across the current selected plan.
     */
    fun deleteRecipeFromTemplate(recipeId: String) {
        Log.d(TAG, "deleteRecipeFromTemplate() called | target recipeId: '$recipeId'")
        viewModelScope.launch {
            val currentPlan = selectedPlan.value ?: run {
                Log.w(TAG, "deleteRecipeFromTemplate() Aborted: selectedPlan is null")
                return@launch
            }

            val updatedDailyPlans = currentPlan.dailyPlans.mapValues { (_, slots) ->
                slots.map { slot ->
                    slot.copy(recipes = slot.recipes.filterNot { it.recipe_id == recipeId })
                }
            }

            val updatedPlan = currentPlan.copy(dailyPlans = updatedDailyPlans)

            try {
                // Await DB update
                planRepository.updatePlan(updatedPlan.toEntity())
                Log.d(TAG, "deleteRecipeFromTemplate(): Updated plan in DB. Refreshing UI flow.")

                // 🌟 Forces selectedPlan and allWeeklyPlans to emit updated data
                refreshPlans()
            } catch (e: Exception) {
                Log.e(TAG, "deleteRecipeFromTemplate() Exception: Failed to update template", e)
            }
        }
    }

    /**
     * Converts raw day strings and nested MealSlotDTOs into populated RealMealSlot domain objects.
     */
    private suspend fun hydrateDailyPlans(
        rawDailyPlans: Map<String, List<MealSlotDTO>>
    ): Map<DayOfWeek, List<RealMealSlot>> = withContext(Dispatchers.IO) {

        rawDailyPlans.entries.associate { (dayStr, slotDTOs) ->
            val dayOfWeek = try {
                DayOfWeek.valueOf(dayStr.uppercase()).also {
                }
            } catch (e: Exception) {
                DayOfWeek.MONDAY
            }

            val realMealSlots = slotDTOs.map { slotDTO ->

                val recipeDeferreds = slotDTO.recipes.map { recipeRef ->
                    async {
                        val result = recipeRepository.getRecipeById(recipeRef.recipeId)
                        result.getOrNull()
                    }
                }

                val populatedRecipes = recipeDeferreds.awaitAll().filterNotNull()

                RealMealSlot(
                    mealType = slotDTO.mealType,
                    recipes = populatedRecipes
                )
            }

            dayOfWeek to realMealSlots
        }
    }

    /**
     * Copies an existing template by ID and saves it as a new private template owned by the current logged-in user.
     */
    fun duplicateTemplate(
        sourcePlanId: String,
        currentUserId: String,
        onSuccess: (newPlanId: String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        Log.d(TAG, "duplicateTemplate() called | sourcePlanId: '$sourcePlanId'")

        viewModelScope.launch {
            try {
                val sourcePlan = allWeeklyPlans.value.find { it.planId == sourcePlanId }
                    ?: run {
                        onError("Source plan not found")
                        return@launch
                    }

                val newPlanId = java.util.UUID.randomUUID().toString()
                val copiedPlan = sourcePlan.copy(
                    planId = newPlanId,
                    planName = "${sourcePlan.planName} (Copy)",
                    userId = currentUserId,
                    public = false
                )

                // Await DB save
                planRepository.saveWeeklyPlan(copiedPlan.toEntity())

                refreshPlans()

                onSuccess(newPlanId)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to copy template")
            }
        }
    }
}
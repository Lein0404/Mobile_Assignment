package com.example.foodieheal.meal_planner.viewModel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.MealSlotDTO
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.WeeklyPlan
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
                    dailyPlans = hydrateDailyPlans(entity.dailyPlans)
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
     * Reactively updates whenever either allWeeklyPlans or currentUserIdFlow emits a change.
     */
    val userWeeklyPlans: StateFlow<List<WeeklyPlan>> = combine(
        allWeeklyPlans,
        currentUserIdFlow
    ) { plans, currentUserId ->
        if (currentUserId.isNullOrBlank()) {
            emptyList()
        } else {
            plans.filter { it.userId == currentUserId }
        }
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
                refreshPlans() // Increments trigger, forcing re-query of observeAllPlans()
                onSuccess()
            } catch (e: Exception) {
                Log.e("TemplateViewModel", "Failed to delete template: ${e.message}", e)
                onError(e.localizedMessage ?: "Failed to delete plan")
            }
        }
    }

    private val planId: String? = savedStateHandle.get<String>("planId")

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPlan: StateFlow<WeeklyPlan?> = allWeeklyPlans
        .mapLatest { plans ->
            if (planId != null) plans.find { it.planId == planId } else null
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun deleteRecipeFromTemplate(recipeId: String) {
        viewModelScope.launch {
            val currentPlan = selectedPlan.value ?: return@launch

            // Remove recipe with matching ID from all slots
            val updatedDailyPlans = currentPlan.dailyPlans.mapValues { (_, slots) ->
                slots.map { slot ->
                    slot.copy(recipes = slot.recipes.filterNot { it.recipe_id == recipeId })
                }
            }

            val updatedPlan = currentPlan.copy(dailyPlans = updatedDailyPlans)

            // Update repository and refresh state
            planRepository.updateTemplate(updatedPlan)
            refreshPlans()
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
                DayOfWeek.valueOf(dayStr.uppercase())
            } catch (e: Exception) {
                DayOfWeek.MONDAY
            }

            // Hydrate each meal slot for the day concurrently
            val realMealSlots = slotDTOs.map { slotDTO ->
                val recipeDeferreds = slotDTO.recipes.map { recipeRef ->
                    async {
                        recipeRepository.getRecipeById(recipeRef.recipeId).getOrNull()
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
}
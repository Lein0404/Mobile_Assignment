package com.example.foodieheal.meal_planner.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.meal_planner.model.toDomain
import com.example.foodieheal.meal_planner.model.toEntity
import com.example.foodieheal.model.Recipe
import com.example.foodieheal.repository.RecipeRepository
import com.example.foodieheal.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID

class AddEditTemplateViewModel(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    private val recipeRepository: RecipeRepository,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTemplateUiState())
    val uiState: StateFlow<AddTemplateUiState> = _uiState.asStateFlow()

    // Retrieve planId from navigation arguments
    val planId: String? = savedStateHandle.get<String>("planId")
    val isEditMode: Boolean = !planId.isNullOrEmpty()

    init {
        // FIXED BUG 1: Used `planId` instead of non-existent `editingPlanId`
        if (isEditMode && planId != null) {
            loadExistingPlan(planId)
        }
    }

    private fun loadExistingPlan(id: String) {
        viewModelScope.launch {
            _uiState.update { currentState -> currentState.copy(isLoading = true) }
            try {
                val existingPlanDto = planRepository.getWeeklyPlanById(id)
                if (existingPlanDto != null) {

                    // 1. Collect all unique recipe IDs referenced in the saved plan
                    val allRecipeIds = existingPlanDto.dailyPlans.values
                        .flatten()
                        .flatMap { slotDto -> slotDto.recipes.map { ref -> ref.recipeId } }
                        .distinct()

                    // 2. Fetch full Recipe objects using RecipeRepository
                    val recipeList = recipeRepository.getRecipesByIds(allRecipeIds).getOrDefault(emptyList())
                    val recipeMap = recipeList
                        .filter { !it.recipe_id.isNullOrEmpty() }
                        .associateBy { it.recipe_id!! }

                    // 3. Convert DTO to Domain using the recipe map
                    val domainDailyPlans = existingPlanDto.dailyPlans.toDomain(recipeMap)

                    _uiState.update { currentState ->
                        currentState.copy(
                            planName = existingPlanDto.planName,
                            category = existingPlanDto.category,
                            dailyPlans = domainDailyPlans,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { currentState -> currentState.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load template"
                    )
                }
            }
        }
    }

    fun updatePlanName(newName: String) {
        _uiState.update { it.copy(planName = newName) }
    }

    fun updateCategory(newCategory: PlanCategory) {
        _uiState.update { it.copy(category = newCategory) }
    }

    fun updateCategoryByString(categoryName: String) {
        val matchedCategory = PlanCategory.entries.find {
            it.name.equals(categoryName, ignoreCase = true)
        }
        _uiState.update { it.copy(category = matchedCategory) }
    }

    fun addRecipeToSlot(day: DayOfWeek, mealType: MealType, recipe: Recipe) {
        _uiState.update { currentState ->
            val updatedDailyPlans = currentState.dailyPlans.toMutableMap()
            val existingSlots = updatedDailyPlans[day]?.toMutableList() ?: mutableListOf()

            val slotIndex = existingSlots.indexOfFirst { it.mealType == mealType }
            if (slotIndex != -1) {
                val currentSlot = existingSlots[slotIndex]
                if (currentSlot.recipes.none { it.recipe_id == recipe.recipe_id }) {
                    existingSlots[slotIndex] = currentSlot.copy(
                        recipes = currentSlot.recipes + recipe
                    )
                }
            } else {
                existingSlots.add(RealMealSlot(mealType = mealType, recipes = listOf(recipe)))
            }

            updatedDailyPlans[day] = existingSlots
            currentState.copy(dailyPlans = updatedDailyPlans)
        }
    }

    fun removeRecipeFromSlot(day: DayOfWeek, mealType: MealType, recipe: Recipe) {
        _uiState.update { currentState ->
            val updatedDailyPlans = currentState.dailyPlans.toMutableMap()
            val existingSlots = updatedDailyPlans[day]?.toMutableList() ?: return@update currentState

            val slotIndex = existingSlots.indexOfFirst { it.mealType == mealType }
            if (slotIndex != -1) {
                val currentSlot = existingSlots[slotIndex]
                val updatedRecipes = currentSlot.recipes.filterNot { it.recipe_id == recipe.recipe_id }
                existingSlots[slotIndex] = currentSlot.copy(recipes = updatedRecipes)
            }

            updatedDailyPlans[day] = existingSlots
            currentState.copy(dailyPlans = updatedDailyPlans)
        }
    }

    fun saveTemplate() {
        val currentState = _uiState.value

        if (currentState.category == null) {
            _uiState.update { it.copy(errorMessage = "Please select a category") }
            return
        }

        if (currentState.planName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Template name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentUserId = authViewModel.currentUser?.id ?: ""

                // FIXED BUG 4: Keep original `planId` in edit mode; generate UUID only for new plans
                val finalPlanId = if (isEditMode && !planId.isNullOrEmpty()) {
                    planId
                } else {
                    UUID.randomUUID().toString()
                }

                val planToSave = WeeklyPlan(
                    planId = finalPlanId,
                    planName = currentState.planName.trim(),
                    userId = currentUserId,
                    category = currentState.category,
                    dailyPlans = currentState.dailyPlans
                )

                planRepository.insertPlan(planToSave.toEntity())
                _uiState.update { it.copy(isLoading = false, isSavedSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Failed to save template")
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
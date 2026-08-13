package com.example.foodieheal.meal_planner.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.meal_planner.model.toEntity
import com.example.foodieheal.model.Recipe
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
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTemplateUiState())
    val uiState: StateFlow<AddTemplateUiState> = _uiState.asStateFlow()

    // Retrieve planId from navigation arguments (null if creating)
    val editingPlanId: String? = savedStateHandle.get<String>("planId")
    val isEditMode: Boolean = !editingPlanId.isNullOrEmpty()

    // Form state fields
    var planName by mutableStateOf("")
        private set

    var selectedCategory by mutableStateOf(PlanCategory.BALANCED)
        private set

    init {
        if (isEditMode && editingPlanId != null) {
            loadExistingPlan(editingPlanId)
        }
    }

    private fun loadExistingPlan(planId: String) {
        viewModelScope.launch {
            val existingPlan = planRepository.getWeeklyPlanById(planId)
            existingPlan?.let { plan ->
                planName = plan.planName
                selectedCategory = plan.category
                // Pre-fill additional fields/meals here if needed
            }
        }
    }

    fun updatePlanName(newName: String) {
        _uiState.update { it.copy(planName = newName) }
    }

    /**
     * Updates the category directly using a PlanCategory enum.
     */
    fun updateCategory(newCategory: PlanCategory) {
        _uiState.update { it.copy(category = newCategory) }
    }

    /**
     * Helper function to match string output from DropDownList UI component.
     */
    fun updateCategoryByString(categoryName: String) {
        val matchedCategory = PlanCategory.entries.find {
            it.name.equals(categoryName, ignoreCase = true) || it.name.equals(categoryName, ignoreCase = true)
        }
        _uiState.update { it.copy(category = matchedCategory) }
    }

    /**
     * Adds a recipe to a specific day and meal slot.
     */
    fun addRecipeToSlot(day: DayOfWeek, mealType: MealType, recipe: Recipe) {
        _uiState.update { currentState ->
            val updatedDailyPlans = currentState.dailyPlans.toMutableMap()
            val existingSlots = updatedDailyPlans[day]?.toMutableList() ?: mutableListOf()

            val slotIndex = existingSlots.indexOfFirst { it.mealType == mealType }
            if (slotIndex != -1) {
                val currentSlot = existingSlots[slotIndex]
                // Prevent duplicate recipe entries inside the same meal slot
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

    /**
     * Removes a recipe from a specific day and meal slot.
     */
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

    /**
     * Converts state into entity DTO and saves it to the database.
     */
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

                val newPlan = WeeklyPlan(
                    planId = UUID.randomUUID().toString(),
                    planName = currentState.planName.trim(),
                    userId = currentUserId,
                    category = currentState.category,
                    dailyPlans = currentState.dailyPlans
                )

                planRepository.insertPlan(newPlan.toEntity())
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
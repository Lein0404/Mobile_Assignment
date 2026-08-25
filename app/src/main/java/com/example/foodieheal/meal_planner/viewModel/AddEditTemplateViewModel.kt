    package com.example.foodieheal.meal_planner.viewModel

    import android.util.Log
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
    import com.example.foodieheal.Recipe.Model.Recipe
    import com.example.foodieheal.Recipe.Repo.RecipeRepository
    import com.example.foodieheal.User.viewModel.AuthViewModel
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

        private companion object {
            private const val TAG = "AddEditTemplateVM"
        }

        private val _uiState = MutableStateFlow(AddTemplateUiState())
        val uiState: StateFlow<AddTemplateUiState> = _uiState.asStateFlow()

        // Retrieve planId from navigation arguments
        val planId: String? = savedStateHandle.get<String>("planId")
        val isEditMode: Boolean = !planId.isNullOrEmpty()

        init {
            Log.d(TAG, "Initialized ViewModel | isEditMode: $isEditMode, planId: '$planId'")
            if (isEditMode && planId != null) {
                loadExistingPlan(planId)
            }
        }

        private fun loadExistingPlan(id: String) {
            Log.d(TAG, "loadExistingPlan() started for planId: '$id'")
            viewModelScope.launch {
                _uiState.update { currentState -> currentState.copy(isLoading = true) }
                try {
                    val existingPlanDto = planRepository.getWeeklyPlanById(id)
                    if (existingPlanDto != null) {
                        Log.d(TAG, "loadExistingPlan(): Found DTO for '$id'. Plan name: '${existingPlanDto.planName}'")

                        // 1. Collect all unique recipe IDs referenced in the saved plan
                        val allRecipeIds = existingPlanDto.dailyPlans.values
                            .flatten()
                            .flatMap { slotDto -> slotDto.recipes.map { ref -> ref.recipeId } }
                            .distinct()

                        Log.d(TAG, "loadExistingPlan(): Found ${allRecipeIds.size} unique recipe IDs to fetch: $allRecipeIds")

                        // 2. Fetch full Recipe objects using RecipeRepository
                        val recipeList = recipeRepository.getRecipesByIds(allRecipeIds).getOrDefault(emptyList())
                        Log.d(TAG, "loadExistingPlan(): Fetched ${recipeList.size} recipe(s) from RecipeRepository")

                        val recipeMap = recipeList
                            .filter { !it.recipe_id.isNullOrEmpty() }
                            .associateBy { it.recipe_id!! }

                        // 3. Convert DTO to Domain using the recipe map
                        val domainDailyPlans = existingPlanDto.dailyPlans.toDomain(recipeMap)
                        Log.d(TAG, "loadExistingPlan(): Converted daily plans to domain model for ${domainDailyPlans.size} days")

                        _uiState.update { currentState ->
                            currentState.copy(
                                planName = existingPlanDto.planName,
                                category = existingPlanDto.category,
                                isPublic = existingPlanDto.public,
                                dailyPlans = domainDailyPlans,
                                isLoading = false
                            )
                        }
                        Log.d(TAG, "loadExistingPlan(): UI state updated successfully for planId: '$id'")
                    } else {
                        Log.w(TAG, "loadExistingPlan(): No plan found in PlanRepository for planId: '$id'")
                        _uiState.update { currentState -> currentState.copy(isLoading = false) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "loadExistingPlan() Exception while loading plan '$id'", e)
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
            Log.d(TAG, "updatePlanName(): New name = '$newName'")
            _uiState.update { it.copy(planName = newName) }
        }

        fun updateCategory(newCategory: PlanCategory) {
            Log.d(TAG, "updateCategory(): New category = '$newCategory'")
            _uiState.update { it.copy(category = newCategory) }
        }

        fun updateCategoryByString(categoryName: String) {
            Log.d(TAG, "updateCategoryByString(): Attempting to match string = '$categoryName'")
            val matchedCategory = PlanCategory.entries.find {
                it.name.equals(categoryName, ignoreCase = true)
            }
            if (matchedCategory != null) {
                Log.d(TAG, "updateCategoryByString(): Matched category = '$matchedCategory'")
            } else {
                Log.w(TAG, "updateCategoryByString(): No matching PlanCategory found for '$categoryName'")
            }
            _uiState.update { it.copy(category = matchedCategory) }
        }

        fun addRecipeToSlot(day: DayOfWeek, mealType: MealType, recipe: Recipe) {
            Log.d(TAG, "addRecipeToSlot(): Adding recipe '${recipe.recipeName}' (ID: ${recipe.recipe_id}) to $day - $mealType")
            _uiState.update { currentState ->
                val updatedDailyPlans = currentState.dailyPlans.toMutableMap()
                val existingSlots = updatedDailyPlans[day]?.toMutableList() ?: mutableListOf()

                val slotIndex = existingSlots.indexOfFirst { it.mealType == mealType }
                if (slotIndex != -1) {
                    val currentSlot = existingSlots[slotIndex]
                    // Allow duplicate recipes by appending directly without checking 'none'
                    existingSlots[slotIndex] = currentSlot.copy(
                        recipes = currentSlot.recipes + recipe
                    )
                } else {
                    Log.d(TAG, "addRecipeToSlot(): Creating new slot for $day - $mealType")
                    existingSlots.add(RealMealSlot(mealType = mealType, recipes = listOf(recipe)))
                }

                updatedDailyPlans[day] = existingSlots
                currentState.copy(dailyPlans = updatedDailyPlans)
            }
        }

        fun removeRecipeFromSlot(day: DayOfWeek, mealType: MealType, recipe: Recipe) {
            Log.d(TAG, "removeRecipeFromSlot(): Removing first instance of '${recipe.recipeName}' (ID: ${recipe.recipe_id}) from $day - $mealType")
            _uiState.update { currentState ->
                val updatedDailyPlans = currentState.dailyPlans.toMutableMap()
                val existingSlots = updatedDailyPlans[day]?.toMutableList() ?: run {
                    Log.w(TAG, "removeRecipeFromSlot(): No slots found for $day")
                    return@update currentState
                }

                val slotIndex = existingSlots.indexOfFirst { it.mealType == mealType }
                if (slotIndex != -1) {
                    val currentSlot = existingSlots[slotIndex]
                    val currentRecipes = currentSlot.recipes.toMutableList()

                    // Find the first index matching the recipe ID and remove only that item
                    val indexToRemove = currentRecipes.indexOfFirst { it.recipe_id == recipe.recipe_id }
                    if (indexToRemove != -1) {
                        currentRecipes.removeAt(indexToRemove)
                        Log.d(TAG, "removeRecipeFromSlot(): Removed recipe at index $indexToRemove. New size: ${currentRecipes.size}")
                        existingSlots[slotIndex] = currentSlot.copy(recipes = currentRecipes)
                    } else {
                        Log.w(TAG, "removeRecipeFromSlot(): Recipe ID '${recipe.recipe_id}' not found in $day - $mealType")
                    }
                } else {
                    Log.w(TAG, "removeRecipeFromSlot(): Slot with mealType $mealType not found on $day")
                }

                updatedDailyPlans[day] = existingSlots
                currentState.copy(dailyPlans = updatedDailyPlans)
            }
        }

        fun updateIsPublic(isPublic: Boolean) {
            Log.d(TAG, "updateIsPublic(): Setting public visibility to $isPublic")
            _uiState.update { currentState ->
                currentState.copy(isPublic = isPublic)
            }
        }

        fun saveTemplate() {
            val currentState = _uiState.value
            Log.d(TAG, "saveTemplate() called | isEditMode: $isEditMode, planName: '${currentState.planName}', category: ${currentState.category}")

            if (currentState.category == null) {
                val errMsg = "Please select a category"
                Log.w(TAG, "saveTemplate() Validation failed: $errMsg")
                _uiState.update { it.copy(errorMessage = errMsg) }
                return
            }

            if (currentState.planName.isBlank()) {
                val errMsg = "Template name cannot be empty"
                Log.w(TAG, "saveTemplate() Validation failed: $errMsg")
                _uiState.update { it.copy(errorMessage = errMsg) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                try {
                    val currentUserId = authViewModel.currentUser?.id ?: ""
                    Log.d(TAG, "saveTemplate(): Saving with userId: '$currentUserId'")

                    val finalPlanId = if (isEditMode && !planId.isNullOrEmpty()) {
                        Log.d(TAG, "saveTemplate(): Retaining existing planId: '$planId'")
                        planId
                    } else {
                        val newUuid = UUID.randomUUID().toString()
                        Log.d(TAG, "saveTemplate(): Generated new UUID for planId: '$newUuid'")
                        newUuid
                    }

                    val planToSave = WeeklyPlan(
                        planId = finalPlanId,
                        planName = currentState.planName.trim(),
                        userId = currentUserId,
                        category = currentState.category,
                        dailyPlans = currentState.dailyPlans,
                        public = currentState.isPublic
                    )

                    if (isEditMode) {
                        Log.d(TAG, "saveTemplate(): Saving plan entity (ID: '${planToSave.planId}')")
                        planRepository.saveWeeklyPlan(planToSave.toEntity())
                    } else {
                        Log.d(TAG, "saveTemplate(): Inserting plan entity into PlanRepository (ID: '${planToSave.planId}')")
                        planRepository.insertPlan(planToSave.toEntity())
                    }

                    Log.d(TAG, "saveTemplate(): Template saved successfully")
                    _uiState.update { it.copy(isLoading = false, isSavedSuccess = true) }
                } catch (e: Exception) {
                    Log.e(TAG, "saveTemplate() Exception occurred while saving template", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "Failed to save template"
                        )
                    }
                }
            }
        }

        fun clearErrorMessage() {
            Log.d(TAG, "clearErrorMessage() called")
            _uiState.update { it.copy(errorMessage = null) }
        }
    }
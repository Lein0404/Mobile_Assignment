package com.example.foodieheal.meal_planner.viewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Recipe
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.RealMealSlot
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class MealPlannerViewModel(
    private val repository: MealPlannerRepository
) : ViewModel() {

    // Observable UI state for the selected day's meal plan
    var selectedDailyPlan by mutableStateOf<DailyPlan?>(null)
        private set

    // Simple state to show loading status if wanted
    var isLoading by mutableStateOf(false)
        private set

    fun loadPlanForDate(date: LocalDate) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.getDailyPlan(date)

            result.onSuccess { plan ->
                selectedDailyPlan = plan
            }.onFailure { exception ->
                // Log or handle errors here
                selectedDailyPlan = null
            }
            isLoading = false
        }
    }

    fun addRecipeToMeal(date: LocalDate, mealType: MealType, recipe: Recipe) {
        viewModelScope.launch {
            isLoading = true

            // 1. Grab the current plan state, or initialize a clean domain model if it's null
            // Note: The repository handles overwriting the real user_id inside saveDailyPlan()!
            val currentPlan = selectedDailyPlan ?: DailyPlan(
                user_id = "",
                date = date.toString(),
                meals = emptyList()
            )

            // 2. Check if a RealMealSlot for this specific MealType already exists
            val slotExists = currentPlan.meals.any { it.mealType == mealType }

            val updatedMeals = if (slotExists) {
                // Append the recipe to the matching existing category slot
                currentPlan.meals.map { slot ->
                    if (slot.mealType == mealType) {
                        slot.copy(recipes = slot.recipes + recipe)
                    } else {
                        slot
                    }
                }
            } else {
                // Create a brand-new slot if it's the first recipe for that category
                val newSlot = RealMealSlot(mealType = mealType, recipes = listOf(recipe))
                currentPlan.meals + newSlot
            }

            // 3. Construct the updated plan configuration
            val updatedPlan = currentPlan.copy(meals = updatedMeals)

            // 4. Update the reactive UI layer immediately for seamless state updates
            selectedDailyPlan = updatedPlan

            // 5. Fire off the background persistence transaction to Supabase
            val result = repository.saveDailyPlan(updatedPlan)

            result.onFailure { exception ->
                Log.e("MealPlannerVM", "Failed to save updated meal plan to Supabase", exception)
                // Optional: Re-fetch original plan state here to roll back UI if sync fails
            }

            isLoading = false
        }
    }

    fun deleteRecipeFromMeal(date: LocalDate, mealType: MealType, recipeToDelete: Recipe) {
        viewModelScope.launch {
            val currentPlan = selectedDailyPlan ?: return@launch // Nothing to delete from if null

            // 1. Filter out the specific recipe from the correct meal slot
            val updatedMeals = currentPlan.meals.mapNotNull { slot ->
                if (slot.mealType == mealType) {
                    // Remove only the first instance that matches the target recipe ID
                    val remainingRecipes = slot.recipes.filterIndexed { _, recipe ->
                        recipe.recipe_id != recipeToDelete.recipe_id
                    }

                    // If there are still recipes left in this slot, keep the slot updated
                    if (remainingRecipes.isNotEmpty()) {
                        slot.copy(recipes = remainingRecipes)
                    } else {
                        null // If no recipes left, drop this meal category slot completely
                    }
                } else {
                    slot
                }
            }

            if (updatedMeals.isEmpty()) {
                // 2. If no meals are left at all for this day, completely wipe it from DB
                selectedDailyPlan = null
                repository.deleteDailyPlan(date)
            } else {
                // 3. Otherwise, update the plan configuration
                val updatedPlan = currentPlan.copy(meals = updatedMeals)

                // Update reactive state for immediate UI reflection
                selectedDailyPlan = updatedPlan

                // Push changes to Supabase
                repository.saveDailyPlan(updatedPlan)
            }
        }
    }

    fun getCurrentWeekDays(baseDate: LocalDate = LocalDate.now()): List<LocalDate> {
        // Find the Sunday of the current week
        val sunday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        // Generate a list of 7 days from Sunday to Saturday
        return (0..6).map { sunday.plusDays(it.toLong()) }
    }
}
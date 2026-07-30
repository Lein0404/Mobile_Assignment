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
            Log.d("MealPlannerDelete", "=== 🛑 START DELETE PROCESS ===")
            Log.d("MealPlannerDelete", "Target Date: $date | Category: $mealType")
            Log.d("MealPlannerDelete", "Recipe to Delete: ${recipeToDelete.recipeName} (ID: ${recipeToDelete.recipe_id})")

            val currentPlan = selectedDailyPlan
            if (currentPlan == null) {
                Log.w("MealPlannerDelete", "❌ Cancelled: selectedDailyPlan is NULL. Nothing to delete from.")
                return@launch
            }

            // Print state before modification
            Log.d("MealPlannerDelete", "Pre-delete meals slot count: ${currentPlan.meals.size}")
            currentPlan.meals.forEach { slot ->
                Log.d("MealPlannerDelete", " - Slot ${slot.mealType} has ${slot.recipes.size} recipes")
            }

            // 1. Walk through the meal slots and strip exactly ONE matching recipe instance
            val updatedMeals = currentPlan.meals.map { slot ->
                if (slot.mealType == mealType) {
                    val targetIndex = slot.recipes.indexOfFirst { it.recipe_id == recipeToDelete.recipe_id }
                    Log.d("MealPlannerDelete", "Looking for recipe index in $mealType slot. Found index: $targetIndex")

                    if (targetIndex != -1) {
                        val remainingRecipes = slot.recipes.toMutableList().apply {
                            removeAt(targetIndex)
                        }
                        Log.d("MealPlannerDelete", "Successfully removed item. Remaining recipes in $mealType: ${remainingRecipes.size}")
                        slot.copy(recipes = remainingRecipes)
                    } else {
                        Log.w("MealPlannerDelete", "⚠️ Recipe ID ${recipeToDelete.recipe_id} not found in $mealType list!")
                        slot
                    }
                } else {
                    slot
                }
            }

            // 2. Build the new configuration state structure
            val updatedPlan = currentPlan.copy(meals = updatedMeals)

            // 3. Update the observable UI state layout immediately
            Log.d("MealPlannerDelete", "Updating reactive selectedDailyPlan UI state...")
            selectedDailyPlan = updatedPlan

            // 4. Fire the persistence sync pipeline over to Supabase
            Log.d("MealPlannerDelete", "Sending payload to repository.saveDailyPlan()...")
            val result = repository.saveDailyPlan(updatedPlan)

            result.onSuccess {
                Log.d("MealPlannerDelete", "✅ SUCCESS: Supabase storage updated successfully!")
            }

            result.onFailure { error ->
                Log.e("MealPlannerDelete", "💥 FAILURE: Supabase update transaction failed!", error)
            }

            Log.d("MealPlannerDelete", "=== 🛑 END DELETE PROCESS ===")
        }
    }

    fun getCurrentWeekDays(baseDate: LocalDate = LocalDate.now()): List<LocalDate> {
        // Find the Sunday of the current week
        val sunday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        // Generate a list of 7 days from Sunday to Saturday
        return (0..6).map { sunday.plusDays(it.toLong()) }
    }
}
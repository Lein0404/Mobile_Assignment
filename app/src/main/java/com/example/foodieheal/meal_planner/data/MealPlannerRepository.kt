package com.example.foodieheal.meal_planner.data

import android.util.Log
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.meal_planner.model.DailyPlanDTO
import com.example.foodieheal.meal_planner.model.MealSlotDTO
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.RecipeReference
import com.example.foodieheal.Recipe.Model.Recipe
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MealPlannerRepository(
    private val postgrest: Postgrest,
    private val supabaseClient: SupabaseClient
) {
    suspend fun saveDailyPlan(plan: DailyPlan): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User is not logged in!")

            val dto = DailyPlanDTO(
                userId = currentUserId,
                date = plan.date,
                meals = plan.meals.map { domainSlot ->
                    MealSlotDTO(
                        mealType = domainSlot.mealType,
                        recipes = domainSlot.recipes.map { recipe ->
                            RecipeReference(recipeId = recipe.recipe_id?:"")
                        }
                    )
                }
            )

            postgrest.from("daily_plans").upsert(dto)
            Unit
        }
    }

    suspend fun getDailyPlan(date: LocalDate): Result<DailyPlan?> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. AUTH CHECK
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("No authenticated user session found.")

            Log.d("MealPlannerDebug", "Starting fetch for date: $date for user: $currentUserId")

            // 2. FETCH DAILY PLAN DTO
            val rawPlan = supabaseClient.postgrest.from("daily_plans")
                .select {
                    filter {
                        eq("date", date.toString())
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<DailyPlanDTO>()
                .firstOrNull()

            // Safely exit if no plan row exists in the DB yet
            if (rawPlan == null) {
                Log.d("MealPlannerDebug", "No daily plan found in database for date: $date")
                return@runCatching null
            }

            // Extract recipe IDs safely
            val recipeIds = rawPlan.meals?.flatMap { slot ->
                slot.recipes.map { it.recipeId }
            }?.distinct().orEmpty()

            // If no recipes are selected for this day, return an empty initialized plan structure
            if (recipeIds.isEmpty()) {
                return@runCatching DailyPlan(
                    user_id = rawPlan.userId,
                    date = rawPlan.date,
                    meals = MealType.entries.map { type -> RealMealSlot(type, emptyList()) }
                )
            }

            Log.d("MealPlannerDebug", "Found recipe IDs in plan: $recipeIds. Fetching full details...")

            // 3. FETCH DETAILS FOR THOSE RECIPES
            val fetchedRecipes = supabaseClient.postgrest.from("recipes")
                .select {
                    filter {
                        isIn("recipe_id", recipeIds)
                    }
                }
                .decodeList<Recipe>()

            Log.d("MealPlannerDebug", "Successfully fetched ${fetchedRecipes.size} recipes from DB.")

            // 4. MAP TO UI DOMAIN MODEL
            // This ensures all 4 slots exist on the screen even if the database row only had 1 slot populated
            val finalPlan = DailyPlan(
                user_id = rawPlan.userId,
                date = rawPlan.date,
                meals = MealType.entries.map { type ->
                    val matchingSlot = rawPlan.meals?.find { it.mealType == type }
                    RealMealSlot(
                        mealType = type,
                        recipes = matchingSlot?.recipes?.mapNotNull { ref ->
                            fetchedRecipes.find { it.recipe_id == ref.recipeId }
                        } ?: emptyList()
                    )
                }
            )

            finalPlan
        }.onFailure { error ->
            // 🌟 This will catch and print the EXACT serialization or network mismatch error to Logcat!
            Log.e("MealPlannerDebug", "💥 Critical Exception in repository layer!", error)
        }
    }
}
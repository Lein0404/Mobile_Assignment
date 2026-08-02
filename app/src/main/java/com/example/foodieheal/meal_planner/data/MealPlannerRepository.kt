package com.example.foodieheal.meal_planner.data

import android.util.Log
import com.example.foodieheal.Recipe
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.meal_planner.model.DailyPlanDTO
import com.example.foodieheal.meal_planner.model.MealSlotDTO
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.RecipeReference
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
                            RecipeReference(recipeId = recipe.recipe_id)
                        }
                    )
                }
            )

            // Upsert the validated DTO structure
            postgrest.from("daily_plans").upsert(dto)
            Unit
        }
    }

    suspend fun getDailyPlan(date: LocalDate): Result<DailyPlan?> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d("MealPlannerDebug", "1. Starting fetch for date: $date")

            val response = supabaseClient.postgrest.from("daily_plans")
                .select { filter { eq("date", date.toString()) } }

            val rawPlan = response.decodeList<DailyPlanDTO>().firstOrNull()
                ?: return@runCatching null


            // 🛠️ DIAGNOSTIC STEP 3: Bypass filters to see what columns and rows actually exist!
            Log.d("MealPlannerDebug", "🔍 DIAGNOSTIC: Fetching raw unfiltered table data...")
            val diagnosticResponse = supabaseClient.postgrest.from("recipes").select()

            Log.d(
                "MealPlannerDebug",
                "🔍 DIAGNOSTIC: Raw JSON payload from recipes table: ${diagnosticResponse.data}"
            )

            val fetchedRecipes = diagnosticResponse.decodeList<Recipe>()
            Log.d("MealPlannerDebug", "6. Decoded ${fetchedRecipes.size} recipes from DB")

            val finalPlan = DailyPlan(
                user_id = rawPlan.userId,
                date = rawPlan.date,
                meals = rawPlan.meals?.map { slot ->
                    RealMealSlot(
                        mealType = slot.mealType,
                        recipes = slot.recipes.mapNotNull { ref ->
                            fetchedRecipes.find { it.recipe_id == ref.recipeId }
                        }
                    )
                } ?: emptyList()
            )
            finalPlan
        }.onFailure { error ->
            Log.e("MealPlannerDebug", "💥 CRASH/EXCEPTION in repository layer!", error)
        }
    }
}
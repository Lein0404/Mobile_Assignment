package com.example.mobileassignmentloginpart.meal_planner


import android.util.Log
import com.example.mobileassignmentloginpart.Recipe
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MealPlannerRepository(
    private val postgrest: Postgrest,
    private val supabaseClient: io.github.jan.supabase.SupabaseClient
) {
    suspend fun saveDailyPlan(plan: DailyPlan): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Grab the active user ID
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User is not logged in!")

            // 2. Attach it to the plan object
            val planWithUser = plan.copy(user_id = currentUserId)

            // 3. Upsert into Supabase
            postgrest.from("daily_plans").upsert(planWithUser)
            Unit
        }
    }

    suspend fun getDailyPlan(date: LocalDate): Result<DailyPlan?> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d("MealPlannerDebug", "1. Starting fetch for date: $date")

            val response = supabaseClient.postgrest.from("daily_plans")
                .select { filter { eq("date", date.toString()) } }

            val rawPlan = response.decodeList<DailyPlanDTO>().firstOrNull()
            if (rawPlan == null) return@runCatching null

            val allRecipeIds = rawPlan.meals.flatMap { it.recipes }.map { it.recipeId }.distinct()

            // 🛠️ DIAGNOSTIC STEP 3: Bypass filters to see what columns and rows actually exist!
            Log.d("MealPlannerDebug", "🔍 DIAGNOSTIC: Fetching raw unfiltered table data...")
            val diagnosticResponse = supabaseClient.postgrest.from("recipes").select()

            Log.d("MealPlannerDebug", "🔍 DIAGNOSTIC: Raw JSON payload from recipes table: ${diagnosticResponse.data}")

            val fetchedRecipes = diagnosticResponse.decodeList<Recipe>()
            Log.d("MealPlannerDebug", "6. Decoded ${fetchedRecipes.size} recipes from DB")

            val finalPlan = DailyPlan(
                user_id = rawPlan.userId,
                date = rawPlan.date,
                meals = rawPlan.meals.map { slot ->
                    RealMealSlot(
                        mealType = slot.mealType,
                        recipes = slot.recipes.mapNotNull { ref ->
                            val found = fetchedRecipes.find { it.recipe_id == ref.recipeId }
                            found
                        }
                    )
                }
            )
            finalPlan
        }.onFailure { error ->
            Log.e("MealPlannerDebug", "💥 CRASH/EXCEPTION in repository layer!", error)
        }
    }

    /**
     * DELETE: Removes the plan for a specific date, restricted to the logged-in user.
     */
    suspend fun deleteDailyPlan(date: LocalDate): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Check if user is logged in
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User is not authenticated")

            // 2. Execute deletion matching the date and target user identity
            supabaseClient.postgrest.from("daily_plans")
                .delete {
                    filter {
                        eq("date", date)
                        eq("user_id", currentUserId)
                    }
                }
            Unit
        }
    }
}
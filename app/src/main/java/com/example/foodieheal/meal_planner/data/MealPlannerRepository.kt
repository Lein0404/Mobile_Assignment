package com.example.foodieheal.meal_planner.data

import android.util.Log
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.meal_planner.model.DailyPlanDTO
import com.example.foodieheal.meal_planner.model.MealSlotDTO
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.RecipeReference
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.meal_planner.model.WeeklyPlanEntity
import com.example.foodieheal.meal_planner.model.toDomain
import com.example.foodieheal.meal_planner.model.toEntity
import com.example.foodieheal.Recipe.Model.Recipe
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MealPlannerRepository(
    private val postgrest: Postgrest,
    private val supabaseClient: SupabaseClient
) {

    // ==========================================================
    // DAILY PLAN METHODS
    // ==========================================================

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
                            RecipeReference(recipeId = recipe.recipe_id ?: "")
                        }
                    )
                }
            )

            // 🌟 Use upsert with a list to be explicit, and catch network errors
            postgrest.from("daily_plans").upsert(listOf(dto))
            Unit
        }.onFailure { error ->
            Log.e("MealPlannerRepo", "CRITICAL: Failed to save daily plan for ${plan.date}", error)
        }
    }

    suspend fun getDailyPlan(date: LocalDate, userId: String? = null): Result<DailyPlan?> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUserId = userId ?: supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("No authenticated user session found.")

            val rawPlan = postgrest.from("daily_plans")
                .select {
                    filter {
                        eq("date", date.toString())
                        eq("user_id", targetUserId)
                    }
                }
                .decodeList<DailyPlanDTO>()
                .firstOrNull() ?: return@runCatching null

            val recipeIds = rawPlan.meals?.flatMap { slot ->
                slot.recipes.map { it.recipeId }
            }?.distinct().orEmpty()

            if (recipeIds.isEmpty()) {
                return@runCatching DailyPlan(
                    user_id = rawPlan.userId,
                    date = rawPlan.date,
                    meals = MealType.entries.map { type -> RealMealSlot(type, emptyList()) }
                )
            }

            val fetchedRecipes = postgrest.from("recipes")
                .select {
                    filter {
                        isIn("recipe_id", recipeIds)
                    }
                }
                .decodeList<Recipe>()

            DailyPlan(
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
        }.onFailure { error ->
            Log.e("MealPlannerDebug", "Critical Exception in getDailyPlan!", error)
        }
    }

    /**
     * Fetches all daily plans within a date range for a specific user.
     * Efficiently fetches recipes in bulk as well.
     */
    suspend fun getDailyPlansInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        userId: String? = null
    ): Result<List<DailyPlan>> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUserId = userId ?: supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("No authenticated user session found.")

            // 1. Fetch all Plan DTOs in range
            val rawPlans = postgrest.from("daily_plans")
                .select {
                    filter {
                        eq("user_id", targetUserId)
                        gte("date", startDate.toString())
                        lte("date", endDate.toString())
                    }
                }
                .decodeList<DailyPlanDTO>()

            if (rawPlans.isEmpty()) return@runCatching emptyList()

            // 2. Extract all unique recipe IDs for the entire month/range
            val allRecipeIds = rawPlans.flatMap { plan ->
                plan.meals?.flatMap { slot -> slot.recipes.map { it.recipeId } } ?: emptyList()
            }.distinct()

            // 3. Fetch all recipe details in ONE request
            val fetchedRecipes = if (allRecipeIds.isNotEmpty()) {
                postgrest.from("recipes")
                    .select { filter { isIn("recipe_id", allRecipeIds) } }
                    .decodeList<Recipe>()
            } else emptyList()

            val recipeMap = fetchedRecipes.associateBy { it.recipe_id }

            // 4. Map back to Domain objects
            rawPlans.map { dto ->
                DailyPlan(
                    user_id = dto.userId,
                    date = dto.date,
                    meals = MealType.entries.map { type ->
                        val matchingSlot = dto.meals?.find { it.mealType == type }
                        RealMealSlot(
                            mealType = type,
                            recipes = matchingSlot?.recipes?.mapNotNull { ref ->
                                recipeMap[ref.recipeId]
                            } ?: emptyList()
                        )
                    }
                )
            }
        }.onFailure { error ->
            Log.e("MealPlannerRepo", "Failed to fetch range plans from $startDate to $endDate", error)
        }
    }

    // ==========================================================
    // 🌟 WEEKLY PLAN METHODS (NEW & UPDATED FOR PUBLIC FIELD)
    // ==========================================================

    /**
     * Saves or updates a WeeklyPlan (including its `public` field status) in Supabase.
     */
    suspend fun saveWeeklyPlan(weeklyPlan: WeeklyPlan): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User is not logged in!")

            // Convert UI Domain model to Entity DTO (carries the `public` flag)
            val entity = weeklyPlan.copy(userId = currentUserId).toEntity()

            postgrest.from("weekly_plans").upsert(entity)
            Unit
        }.onFailure { error ->
            Log.e("MealPlannerDebug", "Failed to save weekly plan", error)
        }
    }

    /**
     * Toggles/updates only the `public` status of a specific plan in Supabase.
     */
    suspend fun updatePlanVisibility(planId: String, isPublic: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest.from("weekly_plans").update(
                mapOf("is_public" to isPublic) // Make sure column name matches your DB!
            ) {
                filter {
                    eq("plan_id", planId)
                }
            }
            Unit
        }.onFailure { error ->
            Log.e("MealPlannerDebug", "Failed to update plan visibility", error)
        }
    }

    /**
     * Fetches public weekly plans shared by community members.
     */
    suspend fun getPublicWeeklyPlans(): Result<List<WeeklyPlan>> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Query Supabase for plans where is_public == true
            val publicEntities = postgrest.from("weekly_plans")
                .select {
                    filter {
                        eq("is_public", true) // Match column name in Supabase
                    }
                }
                .decodeList<WeeklyPlanEntity>()

            if (publicEntities.isEmpty()) return@runCatching emptyList()

            // 2. Collect all recipe IDs across all public plans
            val recipeIds = publicEntities.flatMap { entity ->
                entity.dailyPlans.values.flatten().flatMap { slot ->
                    slot.recipes.map { it.recipeId }
                }
            }.distinct()

            // 3. Fetch recipe details if needed
            val fetchedRecipes = if (recipeIds.isNotEmpty()) {
                postgrest.from("recipes")
                    .select {
                        filter {
                            isIn("recipe_id", recipeIds)
                        }
                    }
                    .decodeList<Recipe>()
            } else emptyList()

            // 4. Map DTO entities back to Domain UI models
            publicEntities.map { entity ->
                entity.toDomain(fetchedRecipes)
            }
        }.onFailure { error ->
            Log.e("MealPlannerDebug", "Failed to fetch public weekly plans", error)
        }
    }
}
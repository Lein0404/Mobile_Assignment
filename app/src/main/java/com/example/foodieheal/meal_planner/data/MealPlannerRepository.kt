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
import com.example.foodieheal.meal_planner.local.MealPlanDao
import com.example.foodieheal.meal_planner.local.LocalWeeklyPlanEntity
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.local.RecipeDao
import com.example.foodieheal.Recipe.local.toEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

class MealPlannerRepository(
    private val postgrest: Postgrest,
    private val supabaseClient: SupabaseClient,
    private val localDao: MealPlanDao,
    private val recipeDao: RecipeDao
) {

    // ==========================================================
    // 🏠 LOCAL STORAGE METHODS
    // ==========================================================

    suspend fun saveWeeklyPlanLocally(weekStartDate: LocalDate, dailyPlans: List<DailyPlan>): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User is not logged in!")

            // 1. Serialize and save the plan structure
            val json = Json.encodeToString(dailyPlans)
            val entity = LocalWeeklyPlanEntity(
                weekStartDate = weekStartDate.toString(),
                userId = currentUserId,
                planJson = json
            )
            localDao.insertPlan(entity)

            // 2. Extract and cache all recipes locally so they are available offline
            val allRecipes = dailyPlans.flatMap { plan ->
                plan.meals.flatMap { slot -> slot.recipes }
            }.distinctBy { it.recipe_id }

            if (allRecipes.isNotEmpty()) {
                val recipeEntities = allRecipes.map { it.toEntity(Json { ignoreUnknownKeys = true }) }
                recipeDao.insertRecipes(recipeEntities)
            }
            allRecipes
        }.onFailure { error ->
            Log.e("MealPlannerRepo", "Failed to save weekly plan locally", error)
        }
    }

    suspend fun clearLocalData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: ""
            // We don't necessarily want to wipe recipes as they might be used by other parts of the app (bookmarks)
            // But we must wipe the user's specific meal plans.
            // Since our DAO doesn't have a "deleteAllForUser", let's assume we want to clear the whole table or add a query.
            // I'll add a clear query to MealPlanDao next.
            localDao.clearAllPlans() 
            Unit
        }
    }

    suspend fun getLocalWeeklyPlan(weekStartDate: LocalDate): Result<List<DailyPlan>> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User is not logged in!")

            val entity = localDao.getPlan(weekStartDate.toString(), currentUserId) 
                ?: throw Exception("No local data for this week")
            
            val plans = Json.decodeFromString<List<DailyPlan>>(entity.planJson)
            plans
        }.onFailure { error ->
            if (error.message != "No local data for this week") {
                Log.e("MealPlannerRepo", "Failed to fetch local weekly plan", error)
            }
        }
    }

    suspend fun deleteLocalWeeklyPlan(weekStartDate: LocalDate): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User is not logged in!")
            localDao.deletePlan(weekStartDate.toString(), currentUserId)
            Unit
        }
    }

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
                .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                    filter {
                        isIn("recipe_id", recipeIds)
                    }
                }
                .decodeList<Recipe>()

            // 🌟 Populate flattened author fields for local storage & UI consistency
            fetchedRecipes.forEach { recipe ->
                recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
            }

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

            // 3. Fetch all recipe details in ONE request with Author Info Join
            val fetchedRecipes = if (allRecipeIds.isNotEmpty()) {
                postgrest.from("recipes")
                    .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                        filter { isIn("recipe_id", allRecipeIds) }
                    }
                    .decodeList<Recipe>()
            } else emptyList()

            // 🌟 Populate flattened author fields for local storage & UI consistency
            fetchedRecipes.forEach { recipe ->
                recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
            }

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
        }.recover { error ->
            // 🌟 OFFLINE FALLBACK for Month View (Calendar Dots)
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: userId ?: ""
            if (currentUserId.isNotEmpty()) {
                val localWeeks = localDao.getAllPlansForUser(currentUserId)
                val allPlans = localWeeks.flatMap { week ->
                    Json.decodeFromString<List<DailyPlan>>(week.planJson)
                }
                // Filter plans within the requested range
                allPlans.filter { plan ->
                    val date = LocalDate.parse(plan.date)
                    (date.isEqual(startDate) || date.isAfter(startDate)) &&
                    (date.isEqual(endDate) || date.isBefore(endDate))
                }
            } else {
                emptyList()
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
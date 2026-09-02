package com.example.foodieheal.meal_planner.data

import android.util.Log
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.meal_planner.model.DailyPlanDTO
import com.example.foodieheal.meal_planner.model.MealSlotDTO
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.RecipeReference
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

    suspend fun saveWeeklyPlanLocally(
        weekStartDate: LocalDate,
        dailyPlans: List<DailyPlan>
    ): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User is not logged in!")

            // 1. Serialize and save the plan structure
            val json = com.example.foodieheal.SupabaseClient.json
            val jsonStr = json.encodeToString(dailyPlans)
            val entity = LocalWeeklyPlanEntity(
                weekStartDate = weekStartDate.toString(),
                userId = currentUserId,
                planJson = jsonStr
            )
            localDao.insertPlan(entity)

            // 2. Extract and cache all recipes locally so they are available offline
            val allRecipes = dailyPlans.flatMap { plan ->
                plan.meals.flatMap { slot -> slot.recipes }
            }.distinctBy { it.recipe_id }

            if (allRecipes.isNotEmpty()) {
                val recipeEntities = allRecipes.map { it.toEntity(json) }
                recipeDao.insertRecipes(recipeEntities)
            }
            allRecipes
        }.onFailure { error ->
            Log.e("MealPlannerRepo", "Failed to save weekly plan locally", error)
        }
    }

    suspend fun clearLocalData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            localDao.clearAllPlans()
            Unit
        }
    }

    /**
     * Performs a full sync of all user plans from Supabase to Local Room DB.
     * This is useful after a fresh install or login.
     */
    suspend fun syncAllUserPlans(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Fetch all DTOs for this user
            val rawPlans = postgrest.from("daily_plans")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<DailyPlanDTO>()

            if (rawPlans.isEmpty()) return@runCatching Unit

            // 2. Extract all unique recipe IDs
            val allRecipeIds = rawPlans.flatMap { plan ->
                plan.meals?.flatMap { slot -> slot.recipes.map { it.realId } } ?: emptyList()
            }.distinct()

            // 3. Fetch all recipes in bulk (with Join retry logic)
            val fetchedRecipes = if (allRecipeIds.isNotEmpty()) {
                try {
                    postgrest.from("recipes")
                        .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                            filter { isIn("recipe_id", allRecipeIds) }
                        }
                        .decodeList<Recipe>()
                } catch (e: Exception) {
                    Log.d("MealPlannerRepo", "Sync Recipes Join Fallback Triggered: ${e.localizedMessage}")
                    postgrest.from("recipes").select {
                        filter { isIn("recipe_id", allRecipeIds) }
                    }.decodeList<Recipe>()
                }
            } else emptyList()

            fetchedRecipes.forEach { recipe ->
                recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
            }

            // Cache recipes
            if (fetchedRecipes.isNotEmpty()) {
                recipeDao.insertRecipes(fetchedRecipes.map { it.toEntity(com.example.foodieheal.SupabaseClient.json) })
            }

            val recipeMap = fetchedRecipes.associateBy { it.recipe_id }

            // 4. Group plans by Week Start (Monday) - using Flexible Date Parsing
            val domainPlans = rawPlans.mapNotNull { dto ->
                val parsedDate = parseFlexibleDate(dto.date) ?: return@mapNotNull null
                DailyPlan(
                    user_id = dto.userId,
                    date = parsedDate.toString(), // Normalize to ISO YYYY-MM-DD
                    meals = MealType.entries.map { type ->
                        val matchingSlot = dto.meals?.find { it.realType == type }
                        RealMealSlot(
                            mealType = type,
                            recipes = matchingSlot?.recipes?.mapNotNull { ref ->
                                recipeMap[ref.realId]
                            } ?: emptyList()
                        )
                    }
                )
            }

            val plansByWeek = domainPlans.groupBy { plan ->
                LocalDate.parse(plan.date)
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            }

            // 5. Save each week to Room
            plansByWeek.forEach { (weekStart, plans) ->
                saveWeeklyPlanLocally(weekStart, plans)
            }
            
            Log.d("MealPlannerRepo", "Full sync complete for user $userId. Synced ${domainPlans.size} days across ${plansByWeek.size} weeks.")
            Unit
        }.onFailure { error ->
            Log.e("MealPlannerRepo", "Failed to perform full sync for user $userId", error)
        }
    }

    private fun parseFlexibleDate(dateStr: String): LocalDate? {
        val patterns = listOf(
            "yyyy-MM-dd",
            "M/d/yyyy",
            "MM/dd/yyyy",
            "d/M/yyyy",
            "dd/MM/yyyy",
            "yyyy/MM/dd"
        )
        for (pattern in patterns) {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
                return LocalDate.parse(dateStr, formatter)
            } catch (e: Exception) { }
        }
        // Fallback for slashes with variable single/double digits
        return try {
            val parts = dateStr.split("/", "-")
            if (parts.size == 3) {
                if (parts[0].length == 4) { // yyyy-mm-dd
                    LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                } else if (parts[2].length == 4) { // m/d/yyyy
                    LocalDate.of(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
                } else null
            } else null
        } catch (e: Exception) {
            Log.w("MealPlannerRepo", "Failed to parse date: $dateStr")
            null
        }
    }

    suspend fun getLocalWeeklyPlan(weekStartDate: LocalDate): Result<List<DailyPlan>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                    ?: throw Exception("User is not logged in!")

                val entity = localDao.getPlan(weekStartDate.toString(), currentUserId)
                    ?: throw Exception("No local data for this week")

                val plans =
                    com.example.foodieheal.SupabaseClient.json.decodeFromString<List<DailyPlan>>(
                        entity.planJson
                    )
                plans
            }.onFailure { error ->
                if (error.message != "No local data for this week") {
                    Log.e("MealPlannerRepo", "Failed to fetch local weekly plan", error)
                }
            }
        }

    suspend fun deleteLocalWeeklyPlan(weekStartDate: LocalDate): Result<Unit> =
        withContext(Dispatchers.IO) {
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

            //  1. Save to Supabase
            postgrest.from("daily_plans").upsert(listOf(dto))

            //  2. Atomic Sync to Local Room DB
            val weekStart = LocalDate.parse(plan.date)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val localResult = getLocalWeeklyPlan(weekStart)

            val updatedWeekPlans = if (localResult.isSuccess) {
                val currentPlans = localResult.getOrThrow().toMutableList()
                val index = currentPlans.indexOfFirst { it.date == plan.date }
                if (index != -1) {
                    currentPlans[index] = plan
                } else {
                    currentPlans.add(plan)
                }
                currentPlans
            } else {
                listOf(plan)
            }

            saveWeeklyPlanLocally(weekStart, updatedWeekPlans)
            Unit
        }.onFailure { error ->
            Log.e("MealPlannerRepo", "CRITICAL: Failed to save daily plan for ${plan.date}", error)
        }
    }

    suspend fun getDailyPlan(date: LocalDate, userId: String? = null): Result<DailyPlan?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetUserId = userId ?: supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("No authenticated user session found.")

                val rawPlan = try {
                    postgrest.from("daily_plans")
                        .select {
                            filter {
                                or {
                                    eq("date", date.toString())
                                    eq("date", "${date.monthValue}/${date.dayOfMonth}/${date.year}")
                                }
                                eq("user_id", targetUserId)
                            }
                        }
                        .decodeList<DailyPlanDTO>()
                        .firstOrNull()
                } catch (e: Exception) {
                    // Fallback to simpler select if complex filter fails
                    postgrest.from("daily_plans")
                        .select {
                            filter {
                                eq("user_id", targetUserId)
                            }
                        }
                        .decodeList<DailyPlanDTO>()
                        .find { it.date == date.toString() || it.date == "${date.monthValue}/${date.dayOfMonth}/${date.year}" }
                } ?: return@runCatching null

                val recipeIds = rawPlan.meals?.flatMap { slot ->
                    slot.recipes.map { it.realId }
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

                //  Populate flattened author fields for local storage & UI consistency
                fetchedRecipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl =
                        recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }

                //  Cache fetched recipes for offline availability
                if (fetchedRecipes.isNotEmpty()) {
                    recipeDao.insertRecipes(fetchedRecipes.map { it.toEntity(com.example.foodieheal.SupabaseClient.json) })
                }

                DailyPlan(
                    user_id = rawPlan.userId,
                    date = rawPlan.date,
                    meals = MealType.entries.map { type ->
                        val matchingSlot = rawPlan.meals?.find { it.realType == type }
                        RealMealSlot(
                            mealType = type,
                            recipes = matchingSlot?.recipes?.mapNotNull { ref ->
                                fetchedRecipes.find { it.recipe_id == ref.realId }
                            } ?: emptyList()
                        )
                    }
                )
            }.recover { error ->
                //  OFFLINE FALLBACK for single day
                val currentUserId = userId ?: supabaseClient.auth.currentUserOrNull()?.id ?: ""
                if (currentUserId.isNotEmpty()) {
                    val weekStart =
                        date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    val localWeek = getLocalWeeklyPlan(weekStart).getOrNull()
                    localWeek?.find { it.date == date.toString() }
                } else null
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
                    }
                }
                .decodeList<DailyPlanDTO>()
                .filter { dto ->
                    val parsed = parseFlexibleDate(dto.date)
                    parsed != null && (parsed.isEqual(startDate) || parsed.isAfter(startDate)) && (parsed.isEqual(endDate) || parsed.isBefore(endDate))
                }

            if (rawPlans.isEmpty()) return@runCatching emptyList()

            // 2. Extract all unique recipe IDs for the entire month/range
            val allRecipeIds = rawPlans.flatMap { plan ->
                plan.meals?.flatMap { slot -> slot.recipes.map { it.realId } } ?: emptyList()
            }.distinct()

            // 3. Fetch all recipe details in ONE request with Author Info Join
            val fetchedRecipes = if (allRecipeIds.isNotEmpty()) {
                postgrest.from("recipes")
                    .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                        filter { isIn("recipe_id", allRecipeIds) }
                    }
                    .decodeList<Recipe>()
            } else emptyList()

            //  Populate flattened author fields for local storage & UI consistency
            fetchedRecipes.forEach { recipe ->
                recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
            }

            //  Cache fetched recipes for offline availability
            if (fetchedRecipes.isNotEmpty()) {
                recipeDao.insertRecipes(fetchedRecipes.map { it.toEntity(com.example.foodieheal.SupabaseClient.json) })
            }

            val recipeMap = fetchedRecipes.associateBy { it.recipe_id }

            // 4. Map back to Domain objects
            rawPlans.map { dto ->
                DailyPlan(
                    user_id = dto.userId,
                    date = dto.date,
                    meals = MealType.entries.map { type ->
                        val matchingSlot = dto.meals?.find { it.realType == type }
                        RealMealSlot(
                            mealType = type,
                            recipes = matchingSlot?.recipes?.mapNotNull { ref ->
                                recipeMap[ref.realId]
                            } ?: emptyList()
                        )
                    }
                )
            }
        }.recover { error ->
            //  OFFLINE FALLBACK for Month View (Calendar Dots)
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: userId ?: ""
            if (currentUserId.isNotEmpty()) {
                val localWeeks = localDao.getAllPlansForUser(currentUserId)
                val allPlans = localWeeks.flatMap { week ->
                    com.example.foodieheal.SupabaseClient.json.decodeFromString<List<DailyPlan>>(
                        week.planJson
                    )
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
            Log.e(
                "MealPlannerRepo",
                "Failed to fetch range plans from $startDate to $endDate",
                error
            )
        }
    }
}
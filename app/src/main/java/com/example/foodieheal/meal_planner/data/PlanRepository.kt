package com.example.foodieheal.meal_planner.data

import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.WeeklyPlanEntity
import com.example.foodieheal.meal_planner.model.WeeklyPlanMealRoomEntity
import com.example.foodieheal.meal_planner.model.WeeklyPlanRoomEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

class PlanRepository(private val planDao: PlanDao) {

    /**
     * Saves a complete WeeklyPlanEntity across both relational tables safely.
     */
    suspend fun saveWeeklyPlan(planEntity: WeeklyPlanEntity) = withContext(Dispatchers.IO) {
        val metadata = WeeklyPlanRoomEntity(
            planId = planEntity.planId,
            planName = planEntity.planName,
            userId = planEntity.userId,
            category = planEntity.category.dbKey,
            weekStartDateString = planEntity.weekStartDateString
        )

        // Flatten Map<String, List<String>> into a database friendly List of rows
        val mealRows = planEntity.dailyPlans.flatMap { (dateStr, recipeIds) ->
            recipeIds.map { recipeId ->
                WeeklyPlanMealRoomEntity(
                    planId = planEntity.planId,
                    dateString = dateStr,
                    recipeId = recipeId
                )
            }
        }

        planDao.insertFullWeeklyPlan(metadata, mealRows)
    }

    /**
     * Retrieves a unified WeeklyPlanEntity by its ID.
     */
    suspend fun getWeeklyPlanById(planId: String): WeeklyPlanEntity? = withContext(Dispatchers.IO) {
        val meta = planDao.getPlanMetadata(planId) ?: return@withContext null
        val mealRows = planDao.getMealsForPlan(planId)

        // Reconstruct the nested Map<String, List<String>> from individual rows
        val dailyPlansMap = mealRows
            .groupBy { it.dateString }
            .mapValues { entry -> entry.value.map { it.recipeId } }

        WeeklyPlanEntity(
            planName = meta.planName,
            planId = meta.planId,
            userId = meta.userId,
            category = PlanCategory.fromDbKey(meta.category),
            weekStartDateString = meta.weekStartDateString,
            dailyPlans = dailyPlansMap
        )
    }

    /**
     * Observes all plans belonging to a specific category AND user reactively.
     */
    fun observePlansByCategoryAndUser(category: PlanCategory, userId: String): Flow<List<WeeklyPlanEntity>> {
        return planDao.getPlansByCategoryAndUserFlow(category.dbKey, userId)
            .combinePlansWithMeals()
    }

    /**
     * Observes all plans belonging to a specific category reactively (e.g., "high_protein").
     */
    fun observePlansByCategory(category: PlanCategory): Flow<List<WeeklyPlanEntity>> {
        return planDao.getPlansByCategoryFlow(category.dbKey).combinePlansWithMeals()
    }

    fun observeAllPlans(): Flow<List<WeeklyPlanEntity>> {
        return planDao.getAllPlansFlow()
            .combinePlansWithMeals()
    }

    /**
     * Deletes a plan and relies on DELETE CASCADE to automatically purge the child meals.
     */
    suspend fun deletePlan(planId: String) = withContext(Dispatchers.IO) {
        planDao.deleteWeeklyPlan(planId)
    }

    // --- Helper Extension to reconstruct list flows cleanly ---
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<List<WeeklyPlanRoomEntity>>.combinePlansWithMeals(): Flow<List<WeeklyPlanEntity>> {
        //  Just call mapLatest directly here 👇
        return mapLatest { metadataList ->
            if (metadataList.isEmpty()) return@mapLatest emptyList()

            // 1. Collect all plan IDs into a list
            val planIds = metadataList.map { it.planId }

            // 2. Perform a SINGLE batch query to fetch all relevant meals
            val allMealRows = planDao.getMealsForPlansBatch(planIds)

            // 3. Group meals by planId in memory for O(1) lookup
            val mealsByPlanId = allMealRows.groupBy { it.planId }

            // 4. Transform metadata safely without hitting the DB again
            metadataList.map { meta ->
                val planMeals = mealsByPlanId[meta.planId] ?: emptyList()

                val dailyPlansMap = planMeals
                    .groupBy { it.dateString }
                    .mapValues { entry -> entry.value.map { it.recipeId } }

                WeeklyPlanEntity(
                    planName = meta.planName,
                    planId = meta.planId,
                    userId = meta.userId,
                    category = PlanCategory.fromDbKey(meta.category),
                    weekStartDateString = meta.weekStartDateString,
                    dailyPlans = dailyPlansMap
                )
            }
        }.flowOn(Dispatchers.IO)
    }
}
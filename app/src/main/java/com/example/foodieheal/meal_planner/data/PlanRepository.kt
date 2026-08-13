package com.example.foodieheal.meal_planner.data

import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.meal_planner.model.WeeklyPlanEntity
import com.example.foodieheal.meal_planner.model.toEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PlanRepository {

    private val postgrest = SupabaseClient.client.from("weekly_plans")

    /**
     * Inserts a new WeeklyPlanEntity into Supabase.
     */
    suspend fun insertPlan(planEntity: WeeklyPlanEntity) = withContext(Dispatchers.IO) {
        postgrest.insert(planEntity)
    }

    /**
     * Saves or updates a complete WeeklyPlanEntity in Supabase.
     */
    suspend fun saveWeeklyPlan(planEntity: WeeklyPlanEntity) = withContext(Dispatchers.IO) {
        postgrest.upsert(planEntity)
    }

    /**
     * Retrieves a WeeklyPlanEntity by its ID.
     */
    suspend fun getWeeklyPlanById(planId: String): WeeklyPlanEntity? = withContext(Dispatchers.IO) {
        postgrest.select {
            filter {
                eq("planId", planId)
            }
        }.decodeSingleOrNull<WeeklyPlanEntity>()
    }

    /**
     * Fetches all plans belonging to a specific category AND user.
     */
    fun observePlansByCategoryAndUser(category: PlanCategory, userId: String): Flow<List<WeeklyPlanEntity>> = flow {
        val plans = postgrest.select {
            filter {
                eq("category", category.name)
                eq("userId", userId)
            }
        }.decodeList<WeeklyPlanEntity>()

        emit(plans)
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches all plans belonging to a specific category.
     */
    fun observePlansByCategory(category: PlanCategory): Flow<List<WeeklyPlanEntity>> = flow {
        val plans = postgrest.select {
            filter {
                eq("category", category.name)
            }
        }.decodeList<WeeklyPlanEntity>()

        emit(plans)
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches all plans.
     */
    fun observeAllPlans(): Flow<List<WeeklyPlanEntity>> = flow {
        val plans = postgrest.select().decodeList<WeeklyPlanEntity>()
        emit(plans)
    }.flowOn(Dispatchers.IO)

    /**
     * Deletes a plan by ID.
     */
    suspend fun deletePlan(planId: String) = withContext(Dispatchers.IO) {
        postgrest.delete {
            filter {
                eq("planId", planId)
            }
        }
    }
    /**
     * Updates an existing template plan in Supabase.
     */
    suspend fun updateTemplate(plan: WeeklyPlan) = withContext(Dispatchers.IO) {
        val entity = plan.toEntity()
        postgrest.upsert(entity)
    }

}
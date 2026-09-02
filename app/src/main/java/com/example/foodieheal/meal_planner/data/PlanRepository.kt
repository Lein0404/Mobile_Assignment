package com.example.foodieheal.meal_planner.data

import android.util.Log
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
    suspend fun insertPlan(planEntity: WeeklyPlanEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest.insert(planEntity)
            Unit
        }.onFailure { e ->
            Log.e("PlanRepository", "insertPlan failed", e)
        }
    }

    /**
     * Saves or updates a complete WeeklyPlanEntity in Supabase.
     */
    suspend fun saveWeeklyPlan(planEntity: WeeklyPlanEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest.upsert(planEntity)
            Unit
        }.onFailure { e ->
            Log.e("PlanRepository", "saveWeeklyPlan failed", e)
        }
    }

    /**
     * Retrieves a WeeklyPlanEntity by its ID.
     */
    suspend fun getWeeklyPlanById(planId: String): WeeklyPlanEntity? = withContext(Dispatchers.IO) {
        runCatching {
            postgrest.select {
                filter {
                    eq("planId", planId)
                }
            }.decodeSingleOrNull<WeeklyPlanEntity>()
        }.getOrNull()
    }

    /**
     * Fetches all plans belonging to a specific category AND user.
     */
    fun observePlansByCategoryAndUser(category: PlanCategory, userId: String): Flow<List<WeeklyPlanEntity>> = flow {
        val result = runCatching {
            postgrest.select {
                filter {
                    eq("category", category.name)
                    eq("userId", userId)
                }
            }.decodeList<WeeklyPlanEntity>()
        }.onFailure { e ->
            Log.e("PlanRepository", "observePlansByCategoryAndUser failed", e)
        }
        emit(result.getOrDefault(emptyList()))
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches all plans belonging to a specific category.
     */
    fun observePlansByCategory(category: PlanCategory): Flow<List<WeeklyPlanEntity>> = flow {
        val result = runCatching {
            postgrest.select {
                filter {
                    eq("category", category.name)
                }
            }.decodeList<WeeklyPlanEntity>()
        }.onFailure { e ->
            Log.e("PlanRepository", "observePlansByCategory failed", e)
        }
        emit(result.getOrDefault(emptyList()))
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches all plans.
     */
    fun observeAllPlans(): Flow<List<WeeklyPlanEntity>> = flow {
        val result = runCatching {
            postgrest.select().decodeList<WeeklyPlanEntity>()
        }.onFailure { e ->
            Log.e("PlanRepository", "observeAllPlans failed", e)
        }
        emit(result.getOrDefault(emptyList()))
    }.flowOn(Dispatchers.IO)

    suspend fun deletePlan(planId: String) = withContext(Dispatchers.IO) {
        runCatching {
            postgrest.delete {
                filter {
                    eq("planId", planId)
                }
            }
        }.onFailure { e ->
            Log.e("PlanRepository", "deletePlan failed", e)
        }
    }
    /**
     * Updates an existing template plan in Supabase.
     */
    suspend fun updatePlan(entity: WeeklyPlanEntity) = withContext(Dispatchers.IO) {
        runCatching {
            postgrest.update(entity) {
                filter {
                    eq("planId", entity.planId)
                }
            }
        }.onFailure { error ->
            Log.e("PlanRepository", "Failed to update plan ${entity.planId}", error)
        }
    }

}
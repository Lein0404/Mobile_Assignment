package com.example.foodieheal.meal_planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.foodieheal.meal_planner.model.WeeklyPlanMealRoomEntity
import com.example.foodieheal.meal_planner.model.WeeklyPlanRoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    // --- Insert Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanMetadata(plan: WeeklyPlanRoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanMeals(meals: List<WeeklyPlanMealRoomEntity>)

    @Transaction
    suspend fun insertFullWeeklyPlan(plan: WeeklyPlanRoomEntity, meals: List<WeeklyPlanMealRoomEntity>) {
        insertPlanMetadata(plan)
        // Clear old items first if replacing to prevent stale meals if updating an existing plan
        deleteMealsForPlan(plan.planId)
        insertPlanMeals(meals)
    }

    // --- Query Operations ---
    @Query("SELECT * FROM weekly_plans")
    fun getAllPlansFlow(): Flow<List<WeeklyPlanRoomEntity>>
    @Query("SELECT * FROM weekly_plans WHERE category = :categoryKey AND user_id = :userId")
    fun getPlansByCategoryAndUserFlow(categoryKey: String, userId: String): Flow<List<WeeklyPlanRoomEntity>>
    @Query("SELECT * FROM weekly_plan_meals WHERE plan_id IN (:planIds)")
    suspend fun getMealsForPlansBatch(planIds: List<String>): List<WeeklyPlanMealRoomEntity>
    @Query("SELECT * FROM weekly_plans WHERE plan_id = :planId")
    suspend fun getPlanMetadata(planId: String): WeeklyPlanRoomEntity?

    @Query("SELECT * FROM weekly_plan_meals WHERE plan_id = :planId")
    suspend fun getMealsForPlan(planId: String): List<WeeklyPlanMealRoomEntity>

    @Query("SELECT * FROM weekly_plans WHERE category = :categoryKey")
    fun getPlansByCategoryFlow(categoryKey: String): Flow<List<WeeklyPlanRoomEntity>>

    @Query("SELECT * FROM weekly_plans WHERE user_id = :userId")
    fun getPlansForUserFlow(userId: String): Flow<List<WeeklyPlanRoomEntity>>

    // --- Delete Operations ---
    @Query("DELETE FROM weekly_plan_meals WHERE plan_id = :planId")
    suspend fun deleteMealsForPlan(planId: String)

    @Query("DELETE FROM weekly_plans WHERE plan_id = :planId")
    suspend fun deleteWeeklyPlan(planId: String)
}
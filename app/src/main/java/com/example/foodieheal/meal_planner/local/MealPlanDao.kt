package com.example.foodieheal.meal_planner.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MealPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: LocalWeeklyPlanEntity)

    @Query("SELECT * FROM local_weekly_plans WHERE weekStartDate = :startDate AND userId = :userId LIMIT 1")
    suspend fun getPlan(startDate: String, userId: String): LocalWeeklyPlanEntity?

    @Query("DELETE FROM local_weekly_plans WHERE weekStartDate = :startDate AND userId = :userId")
    suspend fun deletePlan(startDate: String, userId: String)

    @Query("SELECT * FROM local_weekly_plans WHERE userId = :userId")
    suspend fun getAllPlansForUser(userId: String): List<LocalWeeklyPlanEntity>

    @Query("DELETE FROM local_weekly_plans")
    suspend fun clearAllPlans()
}

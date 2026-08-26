package com.example.foodieheal.meal_planner.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_weekly_plans")
data class LocalWeeklyPlanEntity(
    @PrimaryKey val weekStartDate: String, // Format: YYYY-MM-DD (Monday of the week)
    val userId: String,
    val planJson: String
)

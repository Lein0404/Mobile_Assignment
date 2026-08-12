package com.example.foodieheal.meal_planner.model

import com.example.foodieheal.model.Recipe
import java.time.LocalDate
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 💻 UI Model Layer: Used directly by your Composables.
 * It contains actual Recipe objects so the UI can instantly show names, images, and calories.
 */
data class WeeklyPlan(
    val planName: String = "",
    val planId: String = "",
    val userId: String = "",
    val category: PlanCategory = PlanCategory.BALANCED,
    val weekStartDate: LocalDate,
    val dailyPlans: Map<LocalDate, List<Recipe>> = emptyMap() // Contains full objects for UI
)

/**
 * 📦 Database Entity Layer: Used for Firestore / Room storage.
 * It strictly saves String IDs to keep data lightweight and fresh.
 */
data class WeeklyPlanEntity(
    val planName: String = "",
    val planId: String = "",
    val userId: String = "",
    val category: PlanCategory = PlanCategory.BALANCED,
    val weekStartDateString: String = "",
    val dailyPlans: Map<String, List<String>> = emptyMap() // 🔑 Key: Date string, Value: List of Recipe IDs
)

// ==========================================
// Extension Converters (Free of ViewModel Dependencies)
// ==========================================

// Convert UI state to clean Database Form
fun WeeklyPlan.toEntity(): WeeklyPlanEntity {
    return WeeklyPlanEntity(
        planName = this.planName,
        planId = this.planId,
        userId = this.userId,
        category = this.category,
        weekStartDateString = this.weekStartDate.toString(),
        dailyPlans = this.dailyPlans.entries.associate { (date, recipes) ->
            date.toString() to recipes.map { it.recipe_id?:"" }
        }
    )
}

@Entity(tableName = "weekly_plans")
data class WeeklyPlanRoomEntity(
    @PrimaryKey @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "plan_name") val planName: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "week_start_date_string") val weekStartDateString: String
)

@Entity(tableName = "weekly_plan_meals")
data class WeeklyPlanMealRoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "date_string") val dateString: String,
    @ColumnInfo(name = "recipe_id") val recipeId: String
)
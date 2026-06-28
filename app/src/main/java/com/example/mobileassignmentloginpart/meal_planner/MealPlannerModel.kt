package com.example.mobileassignmentloginpart.meal_planner

import android.health.connect.datatypes.MealType
import com.example.mobileassignmentloginpart.Recipe
import java.time.DayOfWeek
import java.time.LocalDate

enum class RoutineDay {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

// Represents all meals planned for a single calendar day
data class DailyPlan(
    val date: LocalDate,
    val meals: List<MealSlot>,
    val dailyNotes: String? = null // e.g., "Eating out for dinner"
)

// Represents a specific meal category within a day (e.g., Breakfast, Lunch)
data class MealSlot(
    val mealType: MealType,
    val recipes: List<Recipe>,
    val isCompleted: Boolean = false
)

// Enum for standard meal categories
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}
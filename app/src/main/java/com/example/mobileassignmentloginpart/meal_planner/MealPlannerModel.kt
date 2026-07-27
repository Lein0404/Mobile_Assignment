package com.example.mobileassignmentloginpart.meal_planner

import com.example.mobileassignmentloginpart.Recipe
import java.time.LocalDate

data class DailyPlan(
    val date: LocalDate,
    val meals: List<MealSlot>,
){
    companion object {
        val weeklyMealPlan = listOf(
            DailyPlan(
                date = LocalDate.of(2026, 7, 27),
                meals = listOf(
                    MealSlot(MealType.LUNCH, listOf(Recipe.dummyRecipes[0])),
                    MealSlot(MealType.LUNCH, listOf(Recipe.dummyRecipes[1])),
                    MealSlot(MealType.DINNER, listOf(Recipe.dummyRecipes[2])),
                    MealSlot(MealType.SNACK, listOf(Recipe.dummyRecipes[3]))
                )
            ),

            DailyPlan(
                date = LocalDate.of(2026, 7, 28),
                meals = listOf(
                    MealSlot(MealType.BREAKFAST, listOf(Recipe.dummyRecipes[5])),
                    MealSlot(MealType.LUNCH, listOf(Recipe.dummyRecipes[6])),
                    MealSlot(MealType.DINNER, listOf(Recipe.dummyRecipes[7])),
                    MealSlot(MealType.SNACK, listOf(Recipe.dummyRecipes[8]))
                )
            ),

            DailyPlan(
                date = LocalDate.of(2026, 7, 29),
                meals = listOf(
                    MealSlot(MealType.BREAKFAST, listOf(Recipe.dummyRecipes[9])),
                    MealSlot(MealType.LUNCH, listOf(Recipe.dummyRecipes[10])),
                    MealSlot(MealType.DINNER, listOf(Recipe.dummyRecipes[11])),
                    MealSlot(MealType.SNACK, listOf(Recipe.dummyRecipes[12]))
                )
            ),

            DailyPlan(
                date = LocalDate.of(2026, 7, 30),
                meals = listOf(
                    MealSlot(MealType.BREAKFAST, listOf(Recipe.dummyRecipes[13])),
                    MealSlot(MealType.LUNCH, listOf(Recipe.dummyRecipes[14])),
                    MealSlot(MealType.DINNER, listOf(Recipe.dummyRecipes[9])),
                    MealSlot(MealType.SNACK, listOf(Recipe.dummyRecipes[7]))
                )
            ),

            DailyPlan(
                date = LocalDate.of(2026, 7, 31),
                meals = listOf(
                    MealSlot(MealType.BREAKFAST, listOf(Recipe.dummyRecipes[6])),
                    MealSlot(MealType.LUNCH, listOf(Recipe.dummyRecipes[1])),
                    MealSlot(MealType.DINNER, listOf(Recipe.dummyRecipes[0])),
                    MealSlot(MealType.SNACK, listOf(Recipe.dummyRecipes[14]))
                )
            ),

            DailyPlan(
                date = LocalDate.of(2026, 8, 1),
                meals = listOf(
                    MealSlot(MealType.BREAKFAST, listOf(Recipe.dummyRecipes[3])),
                    MealSlot(MealType.LUNCH, listOf(Recipe.dummyRecipes[7])),
                    MealSlot(MealType.DINNER, listOf(Recipe.dummyRecipes[4])),
                    MealSlot(MealType.SNACK, listOf(Recipe.dummyRecipes[10]))
                )
            ),

            DailyPlan(
                date = LocalDate.of(2026, 8, 2),
                meals = listOf(
                    MealSlot(MealType.BREAKFAST, listOf(Recipe.dummyRecipes[9])),
                    MealSlot(MealType.LUNCH, listOf(Recipe.dummyRecipes[5])),
                    MealSlot(MealType.DINNER, listOf(Recipe.dummyRecipes[6])),
                    MealSlot(MealType.SNACK, listOf(Recipe.dummyRecipes[11]))
                )
            )
        )
        fun findPlanByDate(targetDate: LocalDate): DailyPlan? {
            return weeklyMealPlan.find { it.date == targetDate }
        }
    }
}

// Represents a specific meal category within a day (e.g., Breakfast, Lunch)
data class MealSlot(
    val mealType: MealType,
    val recipes: List<Recipe>,
)

// Enum for standard meal categories
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}
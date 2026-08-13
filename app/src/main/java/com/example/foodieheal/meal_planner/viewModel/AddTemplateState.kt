package com.example.foodieheal.meal_planner.viewModel

import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.RealMealSlot
import java.time.DayOfWeek

data class AddTemplateUiState(
    val planName: String = "",
    val category: PlanCategory? = null,
    val dailyPlans: Map<DayOfWeek, List<RealMealSlot>> = DayOfWeek.entries.associateWith {
        MealType.entries.map { type -> RealMealSlot(mealType = type, recipes = emptyList()) }
    },
    val isLoading: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null
)
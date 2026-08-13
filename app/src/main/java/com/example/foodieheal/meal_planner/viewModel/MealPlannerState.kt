package com.example.foodieheal.meal_planner.viewModel

import java.time.LocalDate

data class WeeklyCalendarState(
    val currentWeekStart: LocalDate = LocalDate.now(),
    val weekDays: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val headerText: String = ""
)
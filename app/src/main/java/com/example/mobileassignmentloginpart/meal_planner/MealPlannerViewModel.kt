package com.example.mobileassignmentloginpart.meal_planner

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class MealPlannerViewModel(
    private val repository: MealPlannerRepository
) : ViewModel() {

    // Observable UI state for the selected day's meal plan
    var selectedDailyPlan by mutableStateOf<DailyPlan?>(null)
        private set

    // Simple state to show loading status if wanted
    var isLoading by mutableStateOf(false)
        private set

    fun loadPlanForDate(date: LocalDate) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.getDailyPlan(date)

            result.onSuccess { plan ->
                selectedDailyPlan = plan
            }.onFailure { exception ->
                // Log or handle errors here
                selectedDailyPlan = null
            }
            isLoading = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentWeekDays(baseDate: LocalDate = LocalDate.now()): List<LocalDate> {
        // Find the Sunday of the current week
        val sunday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        // Generate a list of 7 days from Sunday to Saturday
        return (0..6).map { sunday.plusDays(it.toLong()) }
    }
}
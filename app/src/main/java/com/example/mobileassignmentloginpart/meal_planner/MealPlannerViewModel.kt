package com.example.mobileassignmentloginpart.meal_planner

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class MealPlannerViewModel {

//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getTodayMeals(allRoutineSets: List<MealPlan>): List<MealPlan> {
//        // 1. Get today's actual date
//        val today = LocalDate.now()
//
//        // 2. Map Java's day of the week to your DayOfWeek enum/type
//        val currentDay = when (today.dayOfWeek) {
//            java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
//            java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
//            java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
//            java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
//            java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
//            java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
//            java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
//            else -> null
//        }
//
//        // 3. Filter the list to return ALL meals matching today (e.g., Breakfast, Lunch, Dinner)
//        return allRoutineSets.filter { it.dayOfWeek == currentDay }
//    }



}

@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentWeekDays(baseDate: LocalDate = LocalDate.now()): List<LocalDate> {
    // Find the Sunday of the current week
    val sunday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    // Generate a list of 7 days from Sunday to Saturday
    return (0..6).map { sunday.plusDays(it.toLong()) }
}
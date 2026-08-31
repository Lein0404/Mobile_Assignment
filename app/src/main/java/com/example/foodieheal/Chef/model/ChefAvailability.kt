package com.example.foodieheal.Chef.model

import androidx.annotation.StringRes
import com.example.foodieheal.MainActivity
import com.example.foodieheal.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.Calendar

enum class TimeSlotKey(
    @get:StringRes val displayNameRes: Int,
    @get:StringRes val timeRangeRes: Int,
    val displayName: String,
    val timeRange: String
) {
    MORNING(R.string.timeslot_morning, R.string.timeslot_morning_range, "Morning", "8 AM - 12 PM"),
    AFTERNOON(R.string.timeslot_afternoon, R.string.timeslot_afternoon_range, "Afternoon", "12 PM - 5 PM"),
    EVENING(R.string.timeslot_evening, R.string.timeslot_evening_range, "Evening", "5 PM - 9 PM")
}

enum class DayOfWeekKey(
    val code: String,
    @get:StringRes val shortNameRes: Int,
    @get:StringRes val fullNameRes: Int,
    val calendarDay: Int,
    val shortName: String,
    val fullName: String
) {
    MON("mon", R.string.day_mon_short, R.string.day_mon_full, Calendar.MONDAY, "Mon", "Monday"),
    TUE("tue", R.string.day_tue_short, R.string.day_tue_full, Calendar.TUESDAY, "Tue", "Tuesday"),
    WED("wed", R.string.day_wed_short, R.string.day_wed_full, Calendar.WEDNESDAY, "Wed", "Wednesday"),
    THU("thu", R.string.day_thu_short, R.string.day_thu_full, Calendar.THURSDAY, "Thu", "Thursday"),
    FRI("fri", R.string.day_fri_short, R.string.day_fri_full, Calendar.FRIDAY, "Fri", "Friday"),
    SAT("sat", R.string.day_sat_short, R.string.day_sat_full, Calendar.SATURDAY, "Sat", "Saturday"),
    SUN("sun", R.string.day_sun_short, R.string.day_sun_full, Calendar.SUNDAY, "Sun", "Sunday");

    companion object {
        fun fromCalendarDay(calDay: Int): DayOfWeekKey {
            return values().firstOrNull { it.calendarDay == calDay } ?: MON
        }
    }
}

@Serializable
data class DayAvailability(
    val morning: Boolean = true,
    val afternoon: Boolean = true,
    val evening: Boolean = true
) {
    fun isSlotAvailable(slot: TimeSlotKey): Boolean = when (slot) {
        TimeSlotKey.MORNING -> morning
        TimeSlotKey.AFTERNOON -> afternoon
        TimeSlotKey.EVENING -> evening
    }

    fun copyWithSlot(slot: TimeSlotKey, enabled: Boolean): DayAvailability = when (slot) {
        TimeSlotKey.MORNING -> copy(morning = enabled)
        TimeSlotKey.AFTERNOON -> copy(afternoon = enabled)
        TimeSlotKey.EVENING -> copy(evening = enabled)
    }

    fun hasAnySlot(): Boolean = morning || afternoon || evening
    fun hasAllSlots(): Boolean = morning && afternoon && evening
}

@Serializable
data class WeeklyAvailability(
    val mon: DayAvailability = DayAvailability(),
    val tue: DayAvailability = DayAvailability(),
    val wed: DayAvailability = DayAvailability(),
    val thu: DayAvailability = DayAvailability(),
    val fri: DayAvailability = DayAvailability(),
    val sat: DayAvailability = DayAvailability(morning = true, afternoon = true, evening = false),
    val sun: DayAvailability = DayAvailability(morning = false, afternoon = false, evening = false)
) {
    fun getDay(day: DayOfWeekKey): DayAvailability = when (day) {
        DayOfWeekKey.MON -> mon
        DayOfWeekKey.TUE -> tue
        DayOfWeekKey.WED -> wed
        DayOfWeekKey.THU -> thu
        DayOfWeekKey.FRI -> fri
        DayOfWeekKey.SAT -> sat
        DayOfWeekKey.SUN -> sun
    }

    fun copyWithDay(day: DayOfWeekKey, dayAvailability: DayAvailability): WeeklyAvailability = when (day) {
        DayOfWeekKey.MON -> copy(mon = dayAvailability)
        DayOfWeekKey.TUE -> copy(tue = dayAvailability)
        DayOfWeekKey.WED -> copy(wed = dayAvailability)
        DayOfWeekKey.THU -> copy(thu = dayAvailability)
        DayOfWeekKey.FRI -> copy(fri = dayAvailability)
        DayOfWeekKey.SAT -> copy(sat = dayAvailability)
        DayOfWeekKey.SUN -> copy(sun = dayAvailability)
    }

    fun isSlotAvailable(day: DayOfWeekKey, slot: TimeSlotKey): Boolean {
        return getDay(day).isSlotAvailable(slot)
    }

    fun toggleSlot(day: DayOfWeekKey, slot: TimeSlotKey): WeeklyAvailability {
        val currentDay = getDay(day)
        val currentSlotVal = currentDay.isSlotAvailable(slot)
        val updatedDay = currentDay.copyWithSlot(slot, !currentSlotVal)
        return copyWithDay(day, updatedDay)
    }

    fun isAvailableToday(): Boolean {
        val today = DayOfWeekKey.fromCalendarDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        return getDay(today).hasAnySlot()
    }

    fun isDateAvailable(localDate: java.time.LocalDate): Boolean {
        val calDay = when (localDate.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> Calendar.MONDAY
            java.time.DayOfWeek.TUESDAY -> Calendar.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> Calendar.THURSDAY
            java.time.DayOfWeek.FRIDAY -> Calendar.FRIDAY
            java.time.DayOfWeek.SATURDAY -> Calendar.SATURDAY
            java.time.DayOfWeek.SUNDAY -> Calendar.SUNDAY
        }
        val dayKey = DayOfWeekKey.fromCalendarDay(calDay)
        return getDay(dayKey).hasAnySlot()
    }

//Validate time slot
    fun validateTimeSlotForDate(
        localDate: java.time.LocalDate,
        startHour: Int,
        endHour: Int
    ): Pair<Boolean, String?> {
        val calDay = when (localDate.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> Calendar.MONDAY
            java.time.DayOfWeek.TUESDAY -> Calendar.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> Calendar.THURSDAY
            java.time.DayOfWeek.FRIDAY -> Calendar.FRIDAY
            java.time.DayOfWeek.SATURDAY -> Calendar.SATURDAY
            java.time.DayOfWeek.SUNDAY -> Calendar.SUNDAY
        }
        val dayKey = DayOfWeekKey.fromCalendarDay(calDay)
        val dayAvail = getDay(dayKey)

        val dayName = MainActivity.appContext?.getString(dayKey.fullNameRes) ?: dayKey.fullName

        if (!dayAvail.hasAnySlot()) {
            val msg = MainActivity.appContext?.getString(R.string.error_chef_off_duty_format, dayName)
                ?: "Chef is off duty on ${dayKey.fullName}s. Please choose an available date."
            return Pair(false, msg)
        }

        // Determine which slots this appointment covers
        val needsMorning = startHour < 12 && endHour > 8
        val needsAfternoon = startHour < 17 && endHour > 12
        val needsEvening = startHour < 21 && endHour > 17

        if (needsMorning && !dayAvail.morning) {
            val msg = MainActivity.appContext?.getString(R.string.error_chef_not_available_morning_format, dayName)
                ?: "Chef is not available in the Morning (8 AM - 12 PM) on ${dayKey.fullName}s."
            return Pair(false, msg)
        }
        if (needsAfternoon && !dayAvail.afternoon) {
            val msg = MainActivity.appContext?.getString(R.string.error_chef_not_available_afternoon_format, dayName)
                ?: "Chef is not available in the Afternoon (12 PM - 5 PM) on ${dayKey.fullName}s."
            return Pair(false, msg)
        }
        if (needsEvening && !dayAvail.evening) {
            val msg = MainActivity.appContext?.getString(R.string.error_chef_not_available_evening_format, dayName)
                ?: "Chef is not available in the Evening (5 PM - 9 PM) on ${dayKey.fullName}s."
            return Pair(false, msg)
        }

        return Pair(true, null)
    }

    fun toJsonElement(): JsonElement {
        return Json.encodeToJsonElement(serializer(), this)
    }

    companion object {
        fun isConfigured(element: JsonElement?): Boolean {
            return element != null && element !is kotlinx.serialization.json.JsonNull
        }

        fun fromJsonElement(element: JsonElement?): WeeklyAvailability {
            if (element == null) return WeeklyAvailability()
            return try {
                Json.decodeFromJsonElement(serializer(), element)
            } catch (e: Exception) {
                WeeklyAvailability()
            }
        }

        fun allEnabled(): WeeklyAvailability {
            val fullDay = DayAvailability(morning = true, afternoon = true, evening = true)
            return WeeklyAvailability(
                mon = fullDay, tue = fullDay, wed = fullDay, thu = fullDay,
                fri = fullDay, sat = fullDay, sun = fullDay
            )
        }

        fun weekdaysOnly(): WeeklyAvailability {
            val fullDay = DayAvailability(morning = true, afternoon = true, evening = true)
            val offDay = DayAvailability(morning = false, afternoon = false, evening = false)
            return WeeklyAvailability(
                mon = fullDay, tue = fullDay, wed = fullDay, thu = fullDay,
                fri = fullDay, sat = offDay, sun = offDay
            )
        }

        fun allDisabled(): WeeklyAvailability {
            val offDay = DayAvailability(morning = false, afternoon = false, evening = false)
            return WeeklyAvailability(
                mon = offDay, tue = offDay, wed = offDay, thu = offDay,
                fri = offDay, sat = offDay, sun = offDay
            )
        }
    }
}

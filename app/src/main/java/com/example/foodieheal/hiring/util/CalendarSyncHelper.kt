package com.example.foodieheal.hiring.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import es.dmoral.toasty.Toasty
import com.example.foodieheal.R
import com.example.foodieheal.hiring.model.AppointmentPricingBreakdown
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarSyncHelper {

    fun addAppointmentToCalendar(
        context: Context,
        title: String,
        description: String,
        location: String,
        dateStr: String,
        startTimeStr: String,
        endTimeStr: String
    ) {
        val (startMillis, endMillis) = computeEventEpochs(dateStr, startTimeStr, endTimeStr)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            putExtra(CalendarContract.Events.HAS_ALARM, 1)
        }

        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, "Add to Calendar"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toasty.custom(context, "Could not open Calendar app.", R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
        }
    }

    private fun computeEventEpochs(
        dateStr: String,
        startTimeStr: String,
        endTimeStr: String
    ): Pair<Long, Long> {
        val parsedDateCal = parseDate(dateStr) ?: Calendar.getInstance()
        val startMin = AppointmentPricingBreakdown.parseTimeToMinutes(startTimeStr) ?: (9 * 60)
        val endMin = AppointmentPricingBreakdown.parseTimeToMinutes(endTimeStr) ?: (startMin + 120)

        val startCal = (parsedDateCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, startMin / 60)
            set(Calendar.MINUTE, startMin % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = (parsedDateCal.clone() as Calendar).apply {
            if (endMin < startMin) {
                add(Calendar.DAY_OF_YEAR, 1) // Overnight
            }
            set(Calendar.HOUR_OF_DAY, (endMin % (24 * 60)) / 60)
            set(Calendar.MINUTE, endMin % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    private fun parseDate(dateStr: String?): Calendar? {
        if (dateStr.isNullOrBlank()) return null
        val clean = dateStr.trim()
        val patterns = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy/MM/dd",
            "MM/dd/yyyy",
            "d MMM yyyy",
            "dd MMM yyyy"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val d = sdf.parse(clean) ?: continue
                return Calendar.getInstance().apply { time = d }
            } catch (_: Exception) {}
        }
        return null
    }
}

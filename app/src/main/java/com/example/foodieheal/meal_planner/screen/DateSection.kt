package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.ui.theme.Green
import io.ktor.util.date.WeekDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.collections.forEach

@Composable
fun DateCard(
    modifier: Modifier = Modifier,
    day: String,
    date: String = "",
    selected: Boolean,
    onClick: () -> Unit
) {
    val cardContainerColor: Color = when(selected) {
        true -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.background
    }
    val textColor: Color = when(selected) {
        true -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onBackground
    }

    Card(
        onClick = onClick,
        modifier = modifier.size(height = 70.dp, width = 51.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            if (!date.isEmpty()) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun WeeklyDateCardRow(
    modifier: Modifier = Modifier,
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEach { date ->
            DateCard(
                day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                date = date.dayOfMonth.toString(),
                modifier = Modifier,
                selected = date == selectedDate,
                onClick = { onDateSelected(date) }
            )
        }
    }
}
@Composable
fun WeeklyDayCardRow(
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = remember { DayOfWeek.entries.toList() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { day ->
            val isSelected = day == selectedDay
            val dayText = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                    .clickable { onDaySelected(day) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayText,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDatePickerDialog(
    initialDate: LocalDate,
    titleText: String,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate.atStartOfDay(ZoneId.of("UTC+8"))
        .toInstant().toEpochMilli()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedLocalDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC+8"))
                            .toLocalDate()
                        onDateSelected(selectedLocalDate)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = { // 🌟 Overriding the default "Select date" title
                Text(
                    text = titleText,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

@Composable
fun CalendarControls(
    headerText: String,
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    onDateBackward: () -> Unit,
    onDateForward: () -> Unit,
    onCalendarClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    topContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Column {
                topContent?.invoke()
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDateBackward) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.desc_calendar_back),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onCalendarClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = stringResource(R.string.desc_calendar_icon),
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onDateForward) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = stringResource(R.string.desc_calendar_forward),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        WeeklyDateCardRow(
            weekDays = weekDays,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        )
    }
}
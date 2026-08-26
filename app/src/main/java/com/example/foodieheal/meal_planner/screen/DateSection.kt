package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel.DayCondition
import com.example.foodieheal.ui.theme.Green
import com.example.foodieheal.ui.theme.LightBlue
import com.example.foodieheal.ui.theme.Orange
import com.example.foodieheal.ui.theme.Red
import com.example.foodieheal.ui.theme.Yellow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DateCard(
    modifier: Modifier = Modifier,
    day: String,
    date: String = "",
    selected: Boolean,
    condition: DayCondition? = null,
    onClick: () -> Unit
) {
    val cardContainerColor: Color = when (selected) {
        true -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.background
    }
    val textColor: Color = when (selected) {
        true -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onBackground
    }

    val dotColor = when (condition) {
        DayCondition.UNDER_INTAKE -> Yellow
        DayCondition.SLIGHTLY_LOW -> LightBlue
        DayCondition.IDEAL -> Green
        DayCondition.SLIGHTLY_HIGH -> Orange
        DayCondition.EXCESS_INTAKE -> Red
        null -> Color.Transparent
    }

    Card(
        onClick = onClick,
        modifier = modifier.size(height = 76.dp, width = 51.dp),
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
            if (date.isNotEmpty()) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            // Condition Dot
            if (condition != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
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
    monthConditions: Map<LocalDate, DayCondition> = emptyMap(),
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
                condition = monthConditions[date],
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
            title = {
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
    monthConditions: Map<LocalDate, DayCondition> = emptyMap(),
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
            monthConditions = monthConditions,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
fun CustomizedDatePickerDialog(
    initialDate: LocalDate = LocalDate.now(),
    mealPlannerViewModel: MealPlannerViewModel,
    maxCalories: Int,
    isRangeMode: Boolean = false,
    title: String? = null,
    onDateSelected: (date: LocalDate) -> Unit, // Returns selected date (or start date if range mode)
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(initialDate) }

    val displayTitle = title ?: if (isRangeMode) {
        stringResource(R.string.select_7_day_range)
    } else {
        stringResource(R.string.select_date)
    }

    // Computes 7-day range only when in range mode
    val selectedRange = remember(selectedDate, isRangeMode) {
        if (isRangeMode && selectedDate != null) {
            selectedDate!!..selectedDate!!.plusDays(6)
        } else null
    }

    LaunchedEffect(currentMonth) {
        mealPlannerViewModel.loadMonthConditions(currentMonth, maxCalories)
        mealPlannerViewModel.prefetchAdjacentMonths(currentMonth, maxCalories)
    }

    val monthConditions = mealPlannerViewModel.monthConditions

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            Button(
                onClick = {
                    selectedDate?.let { date ->
                        onDateSelected(date)
                    }
                    onDismiss()
                },
                enabled = selectedDate != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
        title = {
            Text(
                text = displayTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Month Navigation Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row {
                        IconButton(
                            onClick = { currentMonth = currentMonth.minusMonths(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.desc_calendar_back)
                            )
                        }
                        IconButton(
                            onClick = { currentMonth = currentMonth.plusMonths(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_forward),
                                contentDescription = stringResource(R.string.desc_calendar_forward),
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

                // Days of Week Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date Grid calculation
                val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value
                val emptyLeadingSpaces = firstDayOfWeek - 1
                val totalDaysInMonth = currentMonth.lengthOfMonth()
                val totalCells = emptyLeadingSpaces + totalDaysInMonth

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(totalCells) { index ->
                        if (index >= emptyLeadingSpaces) {
                            val dayOfMonth = index - emptyLeadingSpaces + 1
                            val date = currentMonth.atDay(dayOfMonth)

                            val condition = monthConditions[date]

                            // Selection state mapping
                            val isStart = if (isRangeMode) date == selectedRange?.start else date == selectedDate
                            val isEnd = if (isRangeMode) date == selectedRange?.endInclusive else date == selectedDate
                            val isInRange = isRangeMode && selectedRange != null && date in selectedRange

                            CalendarDateCard(
                                date = date,
                                isStart = isStart,
                                isEnd = isEnd,
                                isInRange = isInRange,
                                condition = condition,
                                onClick = { selectedDate = date }
                            )
                        } else {
                            Box(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CalendarDateCard(
    date: LocalDate,
    condition: DayCondition?,
    onClick: () -> Unit,
    isStart: Boolean = false,
    isEnd: Boolean = false,
    isInRange: Boolean = false
) {
    val isToday = date == LocalDate.now()
    val isHighlighted = isStart || isEnd || isInRange

    val backgroundColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    val textColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val cardShape = when {
        isStart && isEnd -> RoundedCornerShape(12.dp)
        isStart -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        isEnd -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        isInRange -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(12.dp)
    }

    val dotColor = when (condition) {
        DayCondition.UNDER_INTAKE -> Yellow
        DayCondition.SLIGHTLY_LOW -> LightBlue
        DayCondition.IDEAL -> Green
        DayCondition.SLIGHTLY_HIGH -> Orange
        DayCondition.EXCESS_INTAKE -> Red
        null -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .height(52.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .background(backgroundColor)
            .then(
                if (isToday && !isHighlighted) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, cardShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isHighlighted || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
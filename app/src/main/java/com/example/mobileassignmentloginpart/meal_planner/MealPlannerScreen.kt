package com.example.mobileassignmentloginpart.meal_planner

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileassignmentloginpart.R
import com.example.mobileassignmentloginpart.ui.theme.MobileAssignmentLoginPartTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material3.*
import java.time.Instant
import java.time.ZoneId

@Composable
fun DateCard(
    modifier: Modifier = Modifier,
    day: String,
    date: String,
    selected: Boolean,
    onClick: () -> Unit // Ensure this is present
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
        onClick = onClick, // <-- Make sure the Card triggers the click callback here!
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
                .fillMaxWidth()
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
            Text(
                text = date,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun WeeklyCalendarRow(
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,     // Receive the callback here
    modifier: Modifier = Modifier
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
                onClick = { onDateSelected(date) } // Pass the clicked date up
            )
        }
    }
}


@Composable
fun CalorieProgressBar(
    currentCalories: Int,
    maxCalories: Int,
    calorieTextColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (currentCalories.toFloat() / maxCalories.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row{
            Text(text = "Today's Calories: ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$currentCalories kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = calorieTextColor
            )
            Text(text = " / $maxCalories kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Horizontal Progress Line
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = calorieTextColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

data class MealItem(
    val name: String,
    val calories: Int,
    val cookingTime: Int,
    @DrawableRes val image: Int
)

@Composable
fun MealCard(
    meal: MealItem,
    onDeleteClick: () -> Unit,
    onClick:() -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(color)
    )
    {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(start = 6.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            onClick = onClick
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(meal.image),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = meal.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            painter = painterResource(R.drawable.fire),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text("${meal.calories} kcal", fontSize = 12.sp, maxLines = 1)

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            painter = painterResource(R.drawable.time),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text("${meal.cookingTime} mins", fontSize = 12.sp, maxLines = 1)
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Delete",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MealSection(
    modifier:Modifier = Modifier,
    title: String,
    meal: MealItem,
    onAddClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    val color:Color = when (title) {
        "Breakfast" -> Color(0XFFF4A260)
        "Lunch" -> Color(0XFF65B960)
        "Dinner" -> Color(0XFF4F6D7A)
        else -> Color.Gray
    }
    val icon:Int = when(title){
        "Breakfast" -> R.drawable.breakfast
        "Lunch" -> R.drawable.lunch
        "Dinner" -> R.drawable.dinner
        else -> R.drawable.breakfast
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                Color(0xFFE8E8E8),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.add_circle),
                    contentDescription = "Add",
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MealCard(
            meal = meal,
            onDeleteClick = onDeleteClick,
            onClick = {/*TODO*/},
            color = color
        )
    }
}

@Composable
fun MealPlannerScreenPreview(viewModel: MealPlannerViewModel, modifier: Modifier) {
    // 1. State for the currently selected day (defaults to today)
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // 2. State to track the active week (defaults to Monday of the current week)
    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY)))
    }

    // 3. Generate the 7 days based on the current week start state
    val weekDays = getCurrentWeekDays(currentWeekStart)

    // 4. Format the header text dynamically (e.g., "13 - 19 Jul 2026")
    val weekEndDate = weekDays.last()
    val headerText = "${weekDays.first().dayOfMonth} - ${weekEndDate.dayOfMonth} ${weekEndDate.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())} ${weekEndDate.year}"

    // State to control showing/hiding the date picker dialog
    var showDatePicker by remember { mutableStateOf(false) }

    // Show Dialog when triggered
    if (showDatePicker) {
        MealDatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { newDate ->
                selectedDate = newDate
                // Snap to the Sunday of that week
                currentWeekStart = newDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Handle edit profile */ },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(70.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = "edit profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(45.dp)
                )
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Box(
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                currentWeekStart = currentWeekStart.minusWeeks(1)
                                selectedDate = selectedDate.minusWeeks(1)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Calendar Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Trigger Calendar Picker Dialog on click
                        IconButton(
                            onClick = { showDatePicker = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.calendar),
                                contentDescription = "Calendar",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                currentWeekStart = currentWeekStart.plusWeeks(1)
                                selectedDate = selectedDate.plusWeeks(1)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward),
                                contentDescription = "Calendar Forward",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                WeeklyCalendarRow(
                    weekDays = weekDays,
                    selectedDate = selectedDate,
                    onDateSelected = { newDate ->
                        selectedDate = newDate
                    },
                    modifier = Modifier
                )

                Spacer(Modifier.height(12.dp))
                CalorieProgressBarPreview()
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
                    elevation = CardDefaults.cardElevation(100.dp)
                ) {
                    MealSectionPreview()

                    val meal = MealItem(
                        name = "Greek Yogurt",
                        calories = 400,
                        cookingTime = 120,
                        image = R.drawable.food
                    )

                    MealSection(
                        title = "Lunch",
                        meal = meal
                    )

                    MealSection(
                        title = "Dinner",
                        meal = meal
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert initial LocalDate to UTC milliseconds for the state
    val initialMillis = initialDate.atStartOfDay(ZoneId.of("UTC"))
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
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDateSelected(selectedLocalDate)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MobileAssignmentLoginPartTheme {
        DateCard(Modifier,"Mon","14",false, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun CalorieProgressBarPreview() {
    MobileAssignmentLoginPartTheme {
        CalorieProgressBar(250,600,Color.Green,Modifier)
    }
}

@Preview(showBackground = true)
@Composable
fun MealSectionPreview() {

    val meal = MealItem(
        name = "Greek Yogurt",
        calories = 200,
        cookingTime = 20,
        image = R.drawable.food
    )

    MealSection(
        title = "Breakfast",
        meal = meal
    )
}
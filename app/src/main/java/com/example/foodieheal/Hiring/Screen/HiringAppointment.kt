package com.example.foodieheal.Hiring.Screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Primary Theme Colors matching your UI
private val BrandOrange = Color(0xFFE65100)
private val LightGreyBackground = Color(0xFFE5E5E5)
private val BorderOrange = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringAppointment(
    onBackClick: () -> Unit,
    onAddAppointmentClick: () -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf(Date()) }

    val weekStartDate = remember(selectedDate) { selectedDate.getStartOfWeek() }
    val weekEndDate = remember(weekStartDate) { weekStartDate.addDays(6) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appointment",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandOrange)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            WeekHeaderSection(
                startDate = weekStartDate,
                endDate = weekEndDate,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                onPreviousWeek = { selectedDate = selectedDate.addDays(-7) },
                onNextWeek = { selectedDate = selectedDate.addDays(7) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DayScheduleSection(
                selectedDate = selectedDate,
                onAddAppointmentClick = onAddAppointmentClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeekHeaderSection(
    startDate: Date,
    endDate: Date,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val weekRangeText = "${startDate.formatTo("d")} - ${endDate.formatTo("d MMM yyyy")}"

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = weekRangeText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousWeek) {
                    Icon(painter = painterResource(R.drawable.ic_arrowback), contentDescription = "Previous Week", tint = Color.Black)
                }
                Icon(
                    painter = painterResource(R.drawable.ic_clock),
                    contentDescription = "Calendar",
                    tint = Color.Black,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = onNextWeek) {
                    Icon(painter = painterResource(R.drawable.ic_arrow_right), contentDescription = "Next Week", tint = Color.Black)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (0..6).forEach { dayOffset ->
                val date = startDate.addDays(dayOffset)
                val isSelected = date.isSameDay(selectedDate)

                DayChip(
                    date = date,
                    isSelected = isSelected,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
private fun DayChip(
    date: Date,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(48.dp)
            .height(64.dp)
            .border(
                width = 1.5.dp,
                color = if (isSelected) BrandOrange else BorderOrange,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = if (isSelected) BrandOrange else Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.formatTo("EEE"),
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.DarkGray
        )
        Text(
            text = date.formatTo("d"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

@Composable
private fun DayScheduleSection(
    selectedDate: Date,
    onAddAppointmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = selectedDate.formatTo("EEEE, d MMM"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                AppointmentCard(
                    title = "9:00am - 11:00am",
                    statusText = "Booked",
                    showAddIcon = false
                )
            }

            item {
                AppointmentCard(
                    title = "Appointment",
                    statusText = "Empty",
                    showAddIcon = true,
                    onAddClick = onAddAppointmentClick
                )
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    title: String,
    statusText: String,
    showAddIcon: Boolean = false,
    onAddClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGreyBackground, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            if (showAddIcon) {
                IconButton(onClick = { onAddClick?.invoke() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_add),
                        contentDescription = "Add Appointment",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    color = BrandOrange,
                    shape = RoundedCornerShape(20.dp)
                )
                .background(Color.White, shape = RoundedCornerShape(20.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

fun Date.formatTo(pattern: String): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(this)
}

// Get Monday of the current week
fun Date.getStartOfWeek(): Date {
    val cal = Calendar.getInstance().apply {
        time = this@getStartOfWeek
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    return cal.time
}

// Add/Subtract days
fun Date.addDays(days: Int): Date {
    val cal = Calendar.getInstance().apply {
        time = this@addDays
        add(Calendar.DAY_OF_YEAR, days)
    }
    return cal.time
}

// Compare two dates (ignoring time)
fun Date.isSameDay(other: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = this@isSameDay }
    val cal2 = Calendar.getInstance().apply { time = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
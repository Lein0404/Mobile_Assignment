package com.example.foodieheal.Hiring.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.Chef.ViewModel.AppointmentsUiState
import com.example.foodieheal.Chef.ViewModel.ChefPortalViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.screen.MealDatePickerDialog
import com.example.foodieheal.meal_planner.screen.WeeklyDateCardRow
import com.example.foodieheal.model.Appointment
import com.example.mobileassignmentloginpart.Model.Chef
import kotlinx.datetime.DayOfWeek
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringAppointment(
    chef: Chef,
    onBackClick: () -> Unit,
    onAddAppointmentClick: (selectedDate: LocalDate) -> Unit,
    viewModel: ChefPortalViewModel = viewModel()
) {
    val hiringViewModel: HiringViewModel = viewModel()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val apptUiState by viewModel.appointmentsUiState.collectAsState()

    LaunchedEffect(chef.chefId) {
        chef.chefId?.let { id ->
            hiringViewModel.fetchAppointmentsForChef(id)
        }
    }

    val chefAppointments by hiringViewModel.chefAppointmentsState.collectAsState()

    // Filter appointments for the selected date
    val chefAppointmentsForSelectedDate = remember(chefAppointments, selectedDate) {
        val formattedSelectedDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val altFormattedSelectedDate =
            selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        chefAppointments.filter { appointment ->
            // Matches if date strings match in standard YYYY-MM-DD or DD/MM/YYYY
            appointment.Date.contains(formattedSelectedDate) ||
                    appointment.Date.contains(altFormattedSelectedDate)
        }
    }

    val startOfWeek = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val weekDays = remember(startOfWeek) {
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    val endOfWeek = weekDays.last()
    val weekRangeText = "${startOfWeek.dayOfMonth} - ${endOfWeek.dayOfMonth} ${endOfWeek.format(DateTimeFormatter.ofPattern("MMM yyyy"))}"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Row (Week range text + Navigation controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekRangeText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Previous Week
                    IconButton(onClick = { selectedDate = selectedDate.minusWeeks(1) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Previous Week",
                            tint = Color.Black
                        )
                    }

                    // Calendar Picker Trigger
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_planner),
                            contentDescription = "Pick Date",
                            tint = Color.Black
                        )
                    }

                    // Next Week
                    IconButton(onClick = { selectedDate = selectedDate.plusWeeks(1) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = "Next Week",
                            tint = Color.Black
                        )
                    }
                }
            }

            WeeklyDateCardRow(
                weekDays = weekDays,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pass real appointments to DayScheduleSection
            DayScheduleSection(
                selectedDate = selectedDate,
                appointments = chefAppointmentsForSelectedDate,
                onAddAppointmentClick = {
                    onAddAppointmentClick(selectedDate)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showDatePicker) {
        MealDatePickerDialog(
            initialDate = selectedDate,
            titleText = "Select Appointment Date",
            onDateSelected = { newDate ->
                selectedDate = newDate
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun DayScheduleSection(
    selectedDate: LocalDate,
    appointments: List<Appointment>,
    onAddAppointmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTitle = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // Button to add new booking for this day
                    IconButton(onClick = onAddAppointmentClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_circle_outline),
                            contentDescription = "Add Booking",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // If no appointments found for this date
            if (appointments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No appointments booked for this date.",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // Dynamically display all appointments booked for this day
                items(appointments) { appointment ->
                    // 🟢 Format time as "Start_Time - End_Time"
                    val timeSlotText = if (!appointment.Start_Time.isNullOrBlank() && !appointment.End_Time.isNullOrBlank()) {
                        "${appointment.Start_Time} - ${appointment.End_Time}"
                    } else if (!appointment.Start_Time.isNullOrBlank()) {
                        appointment.Start_Time
                    } else {
                        "Appointment"
                    }

                    AppointmentCard(
                        title = timeSlotText,
                        statusText = appointment.Status,
                        showAddIcon = false
                    )
                }
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
    // Dynamic status color
    val statusBgColor = when (statusText.lowercase()) {
        "confirmed", "accepted" -> Color(0xFFE8F5E9) // Light Green
        "rejected", "cancelled" -> Color(0xFFFFEBEE) // Light Red
        "pending" -> Color(0xFFFFF8E1) // Light Yellow
        else -> Color.White
    }

    val statusTextColor = when (statusText.lowercase()) {
        "confirmed", "accepted" -> Color(0xFF2E7D32)
        "rejected", "cancelled" -> Color(0xFFC62828)
        "pending" -> Color(0xFFF57F17)
        else -> Color.Black
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            if (showAddIcon) {
                IconButton(onClick = { onAddClick?.invoke() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_circle_outline),
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
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
                .background(statusBgColor, shape = RoundedCornerShape(20.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusText.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = statusTextColor
            )
        }
    }
}
package com.example.foodieheal.hiring.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.R
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.meal_planner.screen.MealDatePickerDialog
import com.example.foodieheal.meal_planner.screen.WeeklyDateCardRow
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.ui.components.formatToAmPm
import com.example.foodieheal.Chef.model.Chef
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringAppointment(
    chef: Chef,
    bookingViewModel: AppointmentBookingViewModel = viewModel(),
    onBackClick: () -> Unit,
    onAddAppointmentClick: (selectedDate: LocalDate) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val chefAppointmentsUiState by bookingViewModel.chefAppointmentsState.collectAsState()

    LaunchedEffect(chef.chefId) {
        val chefId = chef.chefId.ifEmpty { chef.id }
        if (chefId.isNotBlank()) {
            bookingViewModel.fetchAppointmentsForChef(chefId)
        }
    }

    val chefAppointments = chefAppointmentsUiState.appointments

    // Filter appointments for the selected date
    val chefAppointmentsForSelectedDate = remember(chefAppointments, selectedDate) {
        val formattedSelectedDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val altFormattedSelectedDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val altFormattedSelectedDate2 = selectedDate.format(DateTimeFormatter.ofPattern("d/M/yyyy"))

        chefAppointments.filter { appointment ->
            val apptDate = appointment.Date.trim()
            val status = appointment.Status?.lowercase(Locale.US).orEmpty()
            val isActive = status !in listOf("cancelled", "rejected")

            isActive && (
                    apptDate.contains(formattedSelectedDate) ||
                            apptDate.contains(altFormattedSelectedDate) ||
                            apptDate.contains(altFormattedSelectedDate2) ||
                            apptDate == selectedDate.toString()
                    )
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
                        text = stringResource(R.string.title_appointment),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back_button),
                            tint = MaterialTheme.colorScheme.onPrimary
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
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Previous Week
                    IconButton(onClick = { selectedDate = selectedDate.minusWeeks(1) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.cd_previous_week),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Calendar Picker Trigger
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_planner),
                            contentDescription = stringResource(R.string.cd_pick_date),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Next Week
                    IconButton(onClick = { selectedDate = selectedDate.plusWeeks(1) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = stringResource(R.string.cd_next_week),
                            tint = MaterialTheme.colorScheme.onBackground
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

            DayScheduleSection(
                selectedDate = selectedDate,
                appointments = chefAppointmentsForSelectedDate,
                chef = chef,
                onAddAppointmentClick = {
                    if (!selectedDate.isBefore(LocalDate.now())) {
                        onAddAppointmentClick(selectedDate)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showDatePicker) {
        MealDatePickerDialog(
            initialDate = selectedDate,
            titleText = stringResource(R.string.title_select_appointment_date),
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
    chef: Chef,
    onAddAppointmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTitle = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }
    val isPastDate = remember(selectedDate) {
        selectedDate.isBefore(LocalDate.now())
    }

    val weeklyAvail = remember(chef.availability_hours) {
        com.example.foodieheal.Chef.model.WeeklyAvailability.fromJsonElement(chef.availability_hours)
    }
    val isChefAvailableOnDate = remember(weeklyAvail, selectedDate, chef.availability_hours) {
        if (chef.availability_hours == null) true
        else weeklyAvail.isDateAvailable(selectedDate)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
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
                    Column {
                        Text(
                            text = formattedTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isChefAvailableOnDate) {
                            Text(
                                text = "Chef Off Duty on this day",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (!isPastDate && isChefAvailableOnDate) {
                        IconButton(onClick = onAddAppointmentClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add_circle_outline),
                                contentDescription = stringResource(R.string.cd_add_booking),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            if (appointments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!isChefAvailableOnDate) {
                                "Chef is off duty on this day. Please select another date on the calendar."
                            } else {
                                stringResource(R.string.empty_no_appointments_for_date)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(appointments) { appointment ->
                    val startAmPm = formatToAmPm(appointment.Start_Time)
                    val endAmPm = formatToAmPm(appointment.End_Time)
                    val timeSlotText = if (startAmPm.isNotBlank() && endAmPm.isNotBlank()) {
                        stringResource(R.string.time_range_format, startAmPm, endAmPm)
                    } else if (startAmPm.isNotBlank()) {
                        startAmPm
                    } else {
                        stringResource(R.string.label_appointment)
                    }

                    AppointmentCard(
                        title = timeSlotText,
                        statusText = appointment.Status.orEmpty(),
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
    val statusBgColor = when (statusText.lowercase()) {
        "completed", "finished" -> Color(0xFFE3F2FD)
        "confirmed", "accepted" -> Color(0xFFE8F5E9)
        "rejected", "cancelled" -> Color(0xFFFFEBEE)
        "pending" -> Color(0xFFFFF8E1)
        else -> MaterialTheme.colorScheme.surface
    }

    val statusTextColor = when (statusText.lowercase()) {
        "completed", "finished" -> Color(0xFF1565C0)
        "confirmed", "accepted" -> Color(0xFF2E7D32)
        "rejected", "cancelled" -> Color(0xFFC62828)
        "pending" -> Color(0xFFF57F17)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(16.dp))
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
                color = MaterialTheme.colorScheme.onTertiary
            )

            if (showAddIcon) {
                IconButton(onClick = { onAddClick?.invoke() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_circle_outline),
                        contentDescription = stringResource(R.string.add_appointment),
                        tint = MaterialTheme.colorScheme.onTertiary,
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
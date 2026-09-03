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
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.foodieheal.Chef.model.WeeklyAvailability
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.R
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.meal_planner.screen.MealDatePickerDialog
import com.example.foodieheal.meal_planner.screen.WeeklyDateCardRow
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.ui.components.AppointmentListSkeleton
import com.example.foodieheal.ui.components.AppointmentStatusBadge
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
    val weekRangeText = "${startOfWeek.dayOfMonth} - ${endOfWeek.dayOfMonth} ${endOfWeek.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))}"
    val isToday = selectedDate == LocalDate.now()

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
            // Chef Context Summary Header Card
            ChefSummaryHeader(
                chef = chef,
                selectedDate = selectedDate
            )

            // Header Row (Week range text + Today quick jump + Navigation controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = weekRangeText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Quick "Today" Jump Chip
                    if (!isToday) {
                        Surface(
                            onClick = { selectedDate = LocalDate.now() },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_clock),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.hiring_schedule_today),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

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

            // Pull-to-Refresh Schedule Area
            PullToRefreshBox(
                isRefreshing = chefAppointmentsUiState.isLoading,
                onRefresh = {
                    val chefId = chef.chefId.ifEmpty { chef.id }
                    if (chefId.isNotBlank()) {
                        bookingViewModel.fetchAppointmentsForChef(chefId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (chefAppointmentsUiState.isLoading && chefAppointments.isEmpty()) {
                    AppointmentListSkeleton(
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DayScheduleSection(
                        selectedDate = selectedDate,
                        appointments = chefAppointmentsForSelectedDate,
                        chef = chef,
                        onAddAppointmentClick = {
                            if (!selectedDate.isBefore(LocalDate.now())) {
                                onAddAppointmentClick(selectedDate)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
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
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH))
    }
    val isPastDate = remember(selectedDate) {
        selectedDate.isBefore(LocalDate.now())
    }

    val weeklyAvail = remember(chef.availability_hours) {
        WeeklyAvailability.fromJsonElement(chef.availability_hours)
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isChefAvailableOnDate) {
                            Text(
                                text = stringResource(R.string.hiring_chef_off_duty_title),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (appointments.isNotEmpty()) {
                            Text(
                                text = "${appointments.size} scheduled booking${if (appointments.size > 1) "s" else ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
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

            if (!isChefAvailableOnDate) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_clock),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.hiring_chef_off_duty_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.hiring_chef_off_duty_desc),
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else if (appointments.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_planner),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.empty_no_appointments_for_date),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (!isPastDate) "Chef has open availability on this date! Tap below to reserve." else "No appointments were scheduled on this date.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            if (!isPastDate) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onAddAppointmentClick,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add_circle_outline),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.add_booking),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(appointments) { appointment ->
                    AppointmentCard(
                        startTime = appointment.Start_Time,
                        endTime = appointment.End_Time,
                        statusText = appointment.Status.orEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    startTime: String,
    endTime: String,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val startAmPm = formatToAmPm(startTime)
    val endAmPm = formatToAmPm(endTime)
    val timeSlotText = if (startAmPm.isNotBlank() && endAmPm.isNotBlank()) {
        stringResource(R.string.time_range_format, startAmPm, endAmPm)
    } else if (startAmPm.isNotBlank()) {
        startAmPm
    } else {
        stringResource(R.string.label_appointment)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Slot with clock icon badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = timeSlotText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.label_appointment),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Standardized Status Badge
                AppointmentStatusBadge(status = statusText)
            }
        }
    }
}

@Composable
private fun ChefSummaryHeader(
    chef: Chef,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val weeklyAvail = remember(chef.availability_hours) {
        WeeklyAvailability.fromJsonElement(chef.availability_hours)
    }
    val isAvailableOnSelectedDate = remember(weeklyAvail, selectedDate, chef.availability_hours) {
        if (chef.availability_hours == null) true
        else weeklyAvail.isDateAvailable(selectedDate)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chef Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                if (!chef.profilePictureUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = stringResource(R.string.profile_picture),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = chef.name.take(1).uppercase(Locale.ROOT).ifBlank {
                            stringResource(R.string.default_initial_chef)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Chef Name and Detail
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chef.name.ifEmpty { stringResource(R.string.unknown_chef) },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star),
                            contentDescription = stringResource(R.string.rating_star),
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(13.dp)
                        )
                        val rating = chef.averagerating
                        Text(
                            text = if (rating != null && rating > 0.0) String.format(Locale.US, "%.1f", rating) else stringResource(R.string.chef_new_rating),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Hourly rate
                    chef.Pricing?.let { price ->
                        Text(
                            text = stringResource(R.string.rate_per_hour, price.toInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Availability Badge for Selected Date
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isAvailableOnSelectedDate) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isAvailableOnSelectedDate) Color(0xFF2E7D32) else Color(0xFFC62828))
                    )
                    Text(
                        text = if (isAvailableOnSelectedDate) stringResource(R.string.chef_available_today) else stringResource(R.string.hiring_schedule_off_duty),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailableOnSelectedDate) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}
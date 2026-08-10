package com.example.foodieheal.Hiring.Screen

import android.R.attr.timeZone
import android.widget.TimePicker
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.Chef.States
import com.example.foodieheal.Hiring.ViewModel.AppointmentValidationError
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.Hiring.ViewModel.UserAppointmentsUiState
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.DropDownList
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.time.ZoneId
import kotlin.time.ExperimentalTime
import java.time.Instant
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleAppointmentScreen(
    appointmentId: String,
    viewModel: HiringViewModel = viewModel(),
    onBackClick: () -> Unit,
    onRescheduleSuccess: () -> Unit
) {
    val context = LocalContext.current
    val appointmentsState by viewModel.userAppointmentsState.collectAsState()

    // Find target appointment
    val appointment = (appointmentsState as? UserAppointmentsUiState.Success)
        ?.appointments
        ?.find { it.AppointmentID == appointmentId }

    if (appointment == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(appointment.chefId) {
        appointment.chefId?.let { chefId ->
            viewModel.fetchAppointmentsForChef(chefId) // Ensure to loads _chefAppointmentsState
        }
    }

    // Form States (Pre-populated with current appointment values)
    var selectedDate by remember { mutableStateOf(appointment.Date) }
    var startTime by remember { mutableStateOf(appointment.Start_Time) }
    var endTime by remember { mutableStateOf(appointment.End_Time) }
    var address by remember { mutableStateOf(appointment.Address.orEmpty()) }
    var postcode by remember { mutableStateOf(appointment.Postcode.orEmpty()) }
    var selectedState by remember { mutableStateOf(appointment.State.orEmpty()) }
    var servingSize by remember { mutableStateOf(appointment.Serving_Size?.toString().orEmpty()) }
    var description by remember { mutableStateOf(appointment.Note.orEmpty()) }

    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf<Set<AppointmentValidationError>>(emptySet()) }

    // Dialog & Flow States
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val isTimeSlotOccupied = validationErrors.contains(AppointmentValidationError.TimeSlotOccupied)
    val isInvalidTime = validationErrors.contains(AppointmentValidationError.InvalidTime)

    LaunchedEffect(selectedDate, startTime, endTime) {
        if (hasAttemptedSubmit) {
            val errors = viewModel.validateFormValues(
                appointmentTime = "$startTime - $endTime",
                address = address,
                postcode = postcode,
                state = selectedState,
                servingSize = servingSize,
                description = description,
                targetDate = selectedDate,
                currentAppointmentId = appointment.AppointmentID
            )
            validationErrors = errors
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reschedule Booking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(R.drawable.ic_arrowback), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Schedule Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Current Booking Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${appointment.Date} | ${appointment.Start_Time} - ${appointment.End_Time}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Select Date
            FormLabel("Date")
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePickerDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(selectedDate, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(painterResource(R.drawable.ic_calendar), contentDescription = "Select Date")
                }
            }

            // Time Slot Pickers
            FormLabel("Time Range")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = { showStartTimePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Start Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(startTime, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = { showEndTimePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("End Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(endTime, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isTimeSlotOccupied) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_help), // Ensure ic_warning exists, or use Material Icons
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "This time slot is already booked. Please pick a different time or date.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (isInvalidTime) {
                Text(
                    text = "Please enter a valid start and end time.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            // Location Details
            FormLabel("Address")
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Address") },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            FormLabel("Postcode")
            OutlinedTextField(
                value = postcode,
                onValueChange = { if (it.all { char -> char.isDigit() }) postcode = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Postcode (e.g. 10000)") },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )

            DropDownList(
                labelId = R.string.state,
                placeholderId = R.string.select_state,
                selectedValue = selectedState,
                options = States,
                onOptionSelected = { selectedState = it }
            )

            // Serving Size & Description / Notes
            FormLabel("Serving Size (Pax)")
            OutlinedTextField(
                value = servingSize,
                onValueChange = { if (it.all { char -> char.isDigit() }) servingSize = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter number of pax") },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            FormLabel("Notes / Special Requirements")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                placeholder = { Text("Add any notes or dietary preferences...") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔘 Submit Button
            Button(
                onClick = {
                    hasAttemptedSubmit = true

                    validationErrors = viewModel.validateAndReschedule(
                        appointmentId = appointment.AppointmentID.orEmpty(),
                        newDate = selectedDate,
                        newStartTime = startTime,
                        newEndTime = endTime,
                        newAddress = address,
                        newPostcode = postcode,
                        newState = selectedState,
                        newServingSize = servingSize,
                        newDescription = description,
                        onSuccess = {
                            Toast.makeText(context, "Rescheduled successfully!", Toast.LENGTH_SHORT).show()
                            onRescheduleSuccess()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isSubmitting && address.isNotBlank() && postcode.length == 5,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm Reschedule", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        selectedDate = formatter.format(Date(millis))
                    }
                    showDatePickerDialog = false
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Start Time Picker Dialog
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(is24Hour = false)
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val formattedHour = if (timePickerState.hour % 12 == 0) 12 else timePickerState.hour % 12
                    val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                    startTime = String.format(Locale.US, "%02d:%02d %s", formattedHour, timePickerState.minute, amPm)
                    showStartTimePicker = false
                }) { Text("Select") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    // End Time Picker Dialog
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(is24Hour = false)
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val formattedHour = if (timePickerState.hour % 12 == 0) 12 else timePickerState.hour % 12
                    val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                    endTime = String.format(Locale.US, "%02d:%02d %s", formattedHour, timePickerState.minute, amPm)
                    showEndTimePicker = false
                }) { Text("Select") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

// Reusable TimePickerDialog Wrapper for Compose
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
        text = { content() }
    )
}
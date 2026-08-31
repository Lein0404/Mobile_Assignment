package com.example.foodieheal.hiring.screen

import android.widget.Toast
import es.dmoral.toasty.Toasty
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.Chef.States
import com.example.foodieheal.R
import com.example.foodieheal.hiring.model.AppointmentValidationError
import com.example.foodieheal.hiring.model.UserAppointmentsUiState
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.hiring.viewmodel.UserAppointmentViewModel
import com.example.foodieheal.ui.components.CommonInputField
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.ui.components.TimePickerDialog
import com.example.foodieheal.ui.components.formatToAmPm
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleAppointmentScreen(
    appointmentId: String,
    userViewModel: UserAppointmentViewModel = viewModel(),
    bookingViewModel: AppointmentBookingViewModel = viewModel(),
    onBackClick: () -> Unit,
    onRescheduleSuccess: () -> Unit
) {
    val context = LocalContext.current
    val appointmentsState by userViewModel.userAppointmentsState.collectAsState()
    val isNetworkAvailable by userViewModel.isNetworkAvailable.collectAsState()

    // Find target appointment
    val appointment = (appointmentsState as? UserAppointmentsUiState.Success)
        ?.appointments
        ?.find { it.AppointmentID == appointmentId }

    if (appointment == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LaunchedEffect(appointment.chefId) {
        appointment.chefId?.let { chefId ->
            bookingViewModel.fetchAppointmentsForChef(chefId)
        }
    }

    // Form States (Pre-populated with current appointment values)
    var selectedDate by remember { mutableStateOf(appointment.Date) }
    var startTime by remember { mutableStateOf(formatToAmPm(appointment.Start_Time).ifBlank { "09:00 AM" }) }
    var endTime by remember { mutableStateOf(formatToAmPm(appointment.End_Time).ifBlank { "11:00 AM" }) }
    var address by remember { mutableStateOf(appointment.Address.orEmpty()) }
    var postcode by remember { mutableStateOf(appointment.Postcode.orEmpty()) }
    var selectedState by remember { mutableStateOf(appointment.State.orEmpty()) }
    var servingSize by remember { mutableStateOf(appointment.Serving_Size?.toString().orEmpty()) }
    var description by remember { mutableStateOf(appointment.Note.orEmpty()) }

    // Dynamic Price Calculation using AppointmentPricingBreakdown
    val chefHourlyRate = remember(appointment) {
        val originalHours = com.example.foodieheal.hiring.model.AppointmentPricingBreakdown.calculateHours(appointment.Start_Time, appointment.End_Time)
        val origPrice = appointment.Total_Price ?: 0.0
        if (originalHours > 0.0) origPrice / originalHours else origPrice
    }

    val pricingBreakdown = remember(chefHourlyRate, startTime, endTime, selectedState, appointment) {
        com.example.foodieheal.hiring.model.AppointmentPricingBreakdown.calculate(
            chefHourlyRate = chefHourlyRate,
            appointmentTime = "$startTime - $endTime",
            selectedRecipes = emptyList(),
            userState = selectedState,
            chefState = appointment.State
        )
    }
    val recalculatedTotalPrice = pricingBreakdown.finalTotalPrice

    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf<Set<AppointmentValidationError>>(emptySet()) }

    // Dialog & Flow States
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val isTimeSlotOccupied = validationErrors.contains(AppointmentValidationError.TimeSlotOccupied)
    val isInvalidTime = validationErrors.contains(AppointmentValidationError.InvalidTime)

    val successToastMsg = stringResource(R.string.toast_reschedule_success)

    LaunchedEffect(selectedDate, startTime, endTime, address, postcode, selectedState, servingSize, description) {
        if (hasAttemptedSubmit) {
            val errors = bookingViewModel.validateFormValues(
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
                title = {
                    Text(
                        text = stringResource(R.string.reschedule_booking),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back_button)
                        )
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
        ) {
            if (!isNetworkAvailable) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.wifi_off),
                            contentDescription = stringResource(R.string.desc_no_network),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.reschedule_offline_mode),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
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
                            text = stringResource(R.string.label_current_booking_details),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${appointment.Date} | ${formatToAmPm(appointment.Start_Time)} - ${formatToAmPm(appointment.End_Time)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dynamic Total Price Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.label_total_price),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.estimated_price_summary),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "RM %.2f", recalculatedTotalPrice),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Select Date
                FormLabel(stringResource(R.string.label_date))
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
                            Text(
                                text = stringResource(R.string.label_date),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = selectedDate,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = stringResource(R.string.cd_select_date),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Time Slot Pickers
                FormLabel(stringResource(R.string.label_time_range))
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
                            Text(
                                text = stringResource(R.string.label_start_time),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatToAmPm(startTime),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { showEndTimePicker = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.label_end_time),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatToAmPm(endTime),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                                painter = painterResource(R.drawable.ic_help),
                                contentDescription = stringResource(R.string.cd_warning),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.error_time_slot_occupied_reschedule),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (isInvalidTime) {
                    Text(
                        text = stringResource(R.string.error_invalid_time_range),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                CommonInputField(
                    value = address,
                    onValueChange = { address = it },
                    textId = R.string.label_address,
                    placeholder = stringResource(R.string.placeholder_address),
                    isError = hasAttemptedSubmit && address.isBlank(),
                    supportingText = if (hasAttemptedSubmit && address.isBlank()) {
                        { Text(stringResource(R.string.error_empty_address)) }
                    } else null,
                    singleLine = false,
                    maxLines = 3,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                CommonInputField(
                    value = postcode,
                    onValueChange = { input ->
                        postcode = input.filter { it.isDigit() }.take(5)
                    },
                    textId = R.string.label_postcode,
                    placeholder = stringResource(R.string.placeholder_postcode_hint),
                    isError = hasAttemptedSubmit && (postcode.length != 5),
                    supportingText = if (hasAttemptedSubmit && postcode.length != 5) {
                        { Text(stringResource(R.string.error_invalid_postcode)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    DropDownList(
                        labelId = R.string.state,
                        placeholderId = R.string.select_state,
                        selectedValue = selectedState,
                        options = States,
                        onOptionSelected = { selectedState = it ?: "" }
                    )
                    if (hasAttemptedSubmit && selectedState.isBlank()) {
                        Text(
                            text = stringResource(R.string.error_select_state),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                CommonInputField(
                    value = servingSize,
                    onValueChange = { input ->
                        servingSize = input.filter { it.isDigit() }.take(3)
                    },
                    textId = R.string.label_serving_size_pax,
                    placeholder = stringResource(R.string.placeholder_serving_size),
                    isError = hasAttemptedSubmit && (servingSize.toIntOrNull() == null || (servingSize.toIntOrNull() ?: 0) <= 0),
                    supportingText = if (hasAttemptedSubmit && (servingSize.toIntOrNull() == null || (servingSize.toIntOrNull() ?: 0) <= 0)) {
                        { Text(stringResource(R.string.error_invalid_serving_size)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                CommonInputField(
                    value = description,
                    onValueChange = { description = it },
                    textId = R.string.label_notes_requirements,
                    placeholder = stringResource(R.string.placeholder_notes_hint),
                    isError = hasAttemptedSubmit && description.isBlank(),
                    supportingText = if (hasAttemptedSubmit && description.isBlank()) {
                        { Text(stringResource(R.string.error_empty_description)) }
                    } else null,
                    singleLine = false,
                    maxLines = 4,
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Submit Button
                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        val errors = bookingViewModel.validateFormValues(
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

                        if (errors.isEmpty()) {
                            isSubmitting = true
                            userViewModel.rescheduleAppointment(
                                appointmentId = appointment.AppointmentID.orEmpty(),
                                newDate = selectedDate,
                                newStartTime = startTime,
                                newEndTime = endTime,
                                newAddress = address,
                                newPostcode = postcode,
                                newState = selectedState,
                                newServingSize = servingSize.toIntOrNull() ?: 0,
                                newDescription = description,
                                newTotalPrice = recalculatedTotalPrice,
                                onSuccess = {
                                    isSubmitting = false
                                    Toasty.custom(context, successToastMsg, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                                    onRescheduleSuccess()
                                },
                                onError = { errorMsg ->
                                    isSubmitting = false
                                    Toasty.custom(context, errorMsg, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_LONG, true, true).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = isNetworkAvailable && !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.confirm_reschedule),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDatePickerDialog) {
        val todayUtcMillis = remember {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Disable all past dates
                    return utcTimeMillis >= todayUtcMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year >= Calendar.getInstance().get(Calendar.YEAR)
                }
            }
        )

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
                }) { Text(stringResource(R.string.select)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
                }) { Text(stringResource(R.string.select)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
                }) { Text(stringResource(R.string.select)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}
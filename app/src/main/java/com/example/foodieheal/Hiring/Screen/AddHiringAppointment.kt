package com.example.foodieheal.hiring.screen

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.Chef.States
import com.example.foodieheal.Chef.healthPreferencesList
import com.example.foodieheal.R
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.ui.components.CommonInputField
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.ui.components.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentFormScreen(
    viewModel: AppointmentBookingViewModel = viewModel(),
    onBackClick: () -> Unit,
    onSuccessConfirm: () -> Unit
) {
    val selectedChef by viewModel.selectedChef.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    val chefId = viewModel.currentChefId
    val focusManager = LocalFocusManager.current

    // Fetch appointments for this specific chef whenever chef ID or date updates
    LaunchedEffect(chefId, selectedDate) {
        if (chefId.isNotBlank()) {
            viewModel.fetchAppointmentsForChef(chefId)
        }
    }

    var startTimeFormatted by remember { mutableStateOf("09:00 AM") }
    var endTimeFormatted by remember { mutableStateOf("11:00 AM") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState.appointmentTime.isBlank()) {
            viewModel.onAppointmentTimeChanged("$startTimeFormatted - $endTimeFormatted")
        }
    }

    fun updateAppointmentTimeSlot(start: String, end: String) {
        startTimeFormatted = start
        endTimeFormatted = end
        viewModel.onAppointmentTimeChanged("$start - $end")
    }

    val hourlyRate = selectedChef?.Pricing ?: 0.0
    val totalPrice = remember(hourlyRate, uiState.appointmentTime) {
        viewModel.calculateTotalPrice(hourlyRate, uiState.appointmentTime)
    }

    val durationText = remember(uiState.appointmentTime) {
        if (uiState.appointmentTime.contains("-")) {
            val parts = uiState.appointmentTime.split("-").map { it.trim() }
            if (parts.size == 2) {
                val format = SimpleDateFormat("hh:mm a", Locale.US)
                try {
                    val start = format.parse(parts[0])
                    val end = format.parse(parts[1])
                    if (start != null && end != null) {
                        val diffMillis = end.time - start.time
                        val diffHours = diffMillis.toDouble() / (1000 * 60 * 60)
                        if (diffHours > 0) {
                            if (diffHours % 1.0 == 0.0) "${diffHours.toInt()} hr" else String.format(Locale.US, "%.1f hrs", diffHours)
                        } else null
                    } else null
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    val timeError = uiState.timeErrorRes?.let { stringResource(it) }
    val addressError = uiState.addressErrorRes?.let { stringResource(it) }
    val postcodeError = uiState.postcodeErrorRes?.let { stringResource(it) }
    val stateError = uiState.stateErrorRes?.let { stringResource(it) }
    val servingSizeError = uiState.servingSizeErrorRes?.let { stringResource(it) }
    val descriptionError = uiState.descriptionErrorRes?.let { stringResource(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_appointment),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
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
            // Offline Warning
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
                            text = "Offline Mode: Internet required to submit booking",
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_appointment_time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Time Card
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { showStartTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (timeError != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.label_start_time),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = startTimeFormatted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // End Time Card
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { showEndTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (timeError != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.label_end_time),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = endTimeFormatted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (timeError != null) {
                    Text(
                        text = timeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                CommonInputField(
                    value = uiState.address,
                    onValueChange = { viewModel.onAddressChanged(it) },
                    textId = R.string.label_address,
                    placeholder = stringResource(R.string.placeholder_address),
                    isError = addressError != null,
                    supportingText = addressError?.let { msg -> { Text(msg) } },
                    singleLine = false,
                    maxLines = 3,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                CommonInputField(
                    value = uiState.postcode,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(5)
                        viewModel.onPostcodeChanged(digits)
                    },
                    textId = R.string.label_postcode,
                    placeholder = stringResource(R.string.placeholder_postcode),
                    isError = postcodeError != null,
                    supportingText = postcodeError?.let { msg -> { Text(msg) } },
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
                        selectedValue = uiState.state,
                        options = States,
                        onOptionSelected = { viewModel.onStateChanged(it ?: "") }
                    )
                    if (stateError != null) {
                        Text(
                            text = stateError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                CommonInputField(
                    value = uiState.servingSize,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(3)
                        viewModel.onServingSizeChanged(digits)
                    },
                    textId = R.string.label_serving_size,
                    placeholder = stringResource(R.string.placeholder_serving_size),
                    isError = servingSizeError != null,
                    supportingText = servingSizeError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                DropDownList(
                    labelId = R.string.health_pref_label,
                    placeholderId = R.string.select_health_pref,
                    selectedValue = uiState.healthPreference,
                    options = healthPreferencesList,
                    onOptionSelected = { viewModel.onHealthPreferenceChanged(it ?: "") }
                )

                CommonInputField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    textId = R.string.label_description,
                    placeholder = stringResource(R.string.placeholder_description),
                    isError = descriptionError != null,
                    supportingText = descriptionError?.let { msg -> { Text(msg) } },
                    singleLine = false,
                    maxLines = 4,
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.estimated_price_summary),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_hourly_rate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.hourly_rate_format, hourlyRate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_estimated_duration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = durationText ?: "--",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_estimated_price),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.currency_rm_format, totalPrice),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.validateAndSubmit { onSuccessConfirm() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.next),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
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
                    val formatted = String.format(Locale.US, "%02d:%02d %s", formattedHour, timePickerState.minute, amPm)

                    updateAppointmentTimeSlot(formatted, endTimeFormatted)
                    showStartTimePicker = false
                }) {
                    Text(stringResource(R.string.select))
                }
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
                    val formatted = String.format(Locale.US, "%02d:%02d %s", formattedHour, timePickerState.minute, amPm)

                    updateAppointmentTimeSlot(startTimeFormatted, formatted)
                    showEndTimePicker = false
                }) {
                    Text(stringResource(R.string.select))
                }
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
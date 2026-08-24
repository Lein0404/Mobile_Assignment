package com.example.foodieheal.hiring.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.foodieheal.hiring.model.AppointmentValidationError
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.ui.components.TimePickerDialog
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

    fun updateAppointmentTimeSlot(start: String, end: String) {
        startTimeFormatted = start
        endTimeFormatted = end
        viewModel.onAppointmentTimeChanged("$start - $end")
    }

    val hasInvalidTimeError = uiState.errors.contains(AppointmentValidationError.InvalidTime)
    val hasTimeSlotOccupiedError = uiState.errors.contains(AppointmentValidationError.TimeSlotOccupied)

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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Appointment Time Range Section
                FormLabel(stringResource(R.string.label_appointment_time))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Start Time Card
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
                            text = endTimeFormatted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && hasInvalidTimeError,
                message = stringResource(R.string.error_invalid_time_range)
            )

            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && hasTimeSlotOccupiedError,
                message = stringResource(R.string.error_time_slot_occupied)
            )

            // Address Section
            FormLabel(stringResource(R.string.label_address))
            FormInputField(
                value = uiState.address,
                onValueChange = { viewModel.onAddressChanged(it) },
                placeholder = stringResource(R.string.placeholder_address),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidAddress),
                message = stringResource(R.string.error_empty_address)
            )

            // Postcode Section
            FormLabel(stringResource(R.string.label_postcode))
            FormInputField(
                value = uiState.postcode,
                onValueChange = { viewModel.onPostcodeChanged(it) },
                placeholder = stringResource(R.string.placeholder_postcode),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidPostcode),
                message = stringResource(R.string.error_invalid_postcode)
            )

            // State Dropdown
            DropDownList(
                labelId = R.string.state,
                placeholderId = R.string.select_state,
                selectedValue = uiState.state,
                options = States,
                onOptionSelected = { viewModel.onStateChanged(it) }
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidState),
                message = stringResource(R.string.error_select_state)
            )

            // Serving Size Section
            FormLabel(stringResource(R.string.label_serving_size))
            FormInputField(
                value = uiState.servingSize,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        viewModel.onServingSizeChanged(newValue)
                    }
                },
                placeholder = stringResource(R.string.placeholder_serving_size),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidServingSize),
                message = stringResource(R.string.error_invalid_serving_size)
            )

            // Health Preferences Dropdown
            DropDownList(
                labelId = R.string.health_pref_label,
                placeholderId = R.string.select_health_pref,
                selectedValue = uiState.healthPreference,
                options = healthPreferencesList,
                onOptionSelected = { viewModel.onHealthPreferenceChanged(it) }
            )

            // Description Section
            FormLabel(stringResource(R.string.label_description))
            FormInputField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                placeholder = stringResource(R.string.placeholder_description),
                singleLine = false,
                modifier = Modifier.height(110.dp)
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidDescription),
                message = stringResource(R.string.error_empty_description)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.validateAndSubmit { onSuccessConfirm() }
                },
                enabled = uiState.canSubmit,
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun FormInputField(
    value: String,
    onValueChange: (String) -> Unit = {},
    placeholder: String,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly || (onClick != null),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (onClick != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            )
        }
    }
}

// Animated Error Message Helper
@Composable
fun AnimatedErrorMessage(visible: Boolean, message: String) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
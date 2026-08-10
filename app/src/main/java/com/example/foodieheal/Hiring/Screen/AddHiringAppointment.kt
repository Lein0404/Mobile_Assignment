package com.example.foodieheal.Hiring.Screen

import android.app.TimePickerDialog
import android.widget.TimePicker
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.Chef.States
import com.example.foodieheal.Chef.healthPreferencesList
import com.example.foodieheal.Hiring.ViewModel.AppointmentValidationError
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.R
import com.example.foodieheal.SupabaseClient.client
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.viewmodel.AuthViewModel
import io.github.jan.supabase.auth.auth
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentFormScreen(
    viewModel: HiringViewModel = viewModel(),
    onBackClick: () -> Unit,
    onSuccessConfirm: () -> Unit
) {
    val chefId = viewModel.currentChefId

    LaunchedEffect(chefId, viewModel.selectedDate) {
        if (chefId.isNotBlank()) {
            viewModel.fetchAppointmentsForChef(chefId)
        }
    }

    val selectedChef = viewModel.selectedChef
    val hourlyPrice = selectedChef?.Pricing ?: 0.0
    val currentUserId = client.auth.currentUserOrNull()?.id.orEmpty()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Local states for Start & End Time (Parsed from uiState or default)
    var startTimeFormatted by remember { mutableStateOf("09:00 AM") }
    var endTimeFormatted by remember { mutableStateOf("11:00 AM") }

    // Dialog Visibility States
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Helper to update ViewModel time slot
    fun updateAppointmentTimeSlot(start: String, end: String) {
        startTimeFormatted = start
        endTimeFormatted = end
        viewModel.onAppointmentTimeChanged("$start - $end")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appointment",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Back",
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Appointment Time Range
            FormLabel("Appointment Time")

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
                            text = "Start Time",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = startTimeFormatted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
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
                            text = "End Time",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = endTimeFormatted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.hasInvalidTimeError,
                message = "Please select an appointment time range."
            )

            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.hasTimeSlotOccupiedError,
                message = "This time slot is already booked for this chef. Please select another time."
            )

            // Address Fields
            FormLabel("Address")
            FormInputField(
                value = uiState.address,
                onValueChange = { viewModel.onAddressChanged(it) },
                placeholder = "Address",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidAddress),
                message = "Address cannot be empty."
            )

            FormLabel("Postcode")
            FormInputField(
                value = uiState.postcode,
                onValueChange = { viewModel.onPostcodeChanged(it) },
                placeholder = "Postcode",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidPostcode),
                message = "Postcode must contain exactly 5 digits."
            )

            DropDownList(
                labelId = R.string.state,
                placeholderId = R.string.select_state,
                selectedValue = uiState.state,
                options = States,
                onOptionSelected = { viewModel.onStateChanged(it) }
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidState),
                message = "Please select a state."
            )

            // Serving Size & Preferences
            FormLabel("Serving Size")
            FormInputField(
                value = uiState.servingSize,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        viewModel.onServingSizeChanged(newValue)
                    }
                },
                placeholder = "Enter number of pax (e.g. 5)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidServingSize),
                message = "Please enter a valid serving size."
            )

            DropDownList(
                labelId = R.string.health_pref_label,
                placeholderId = R.string.select_health_pref,
                selectedValue = uiState.healthPreference,
                options = healthPreferencesList,
                onOptionSelected = { viewModel.onHealthPreferenceChanged(it) }
            )

            FormLabel("Description")
            FormInputField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                placeholder = "Description",
                singleLine = false,
                modifier = Modifier.height(110.dp)
            )
            AnimatedErrorMessage(
                visible = uiState.hasAttemptedSubmit && uiState.errors.contains(AppointmentValidationError.InvalidDescription),
                message = "Description cannot be empty."
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm Button
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
                    text = "Next",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
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
                    val formatted = String.format(Locale.US, "%02d:%02d %s", formattedHour, timePickerState.minute, amPm)

                    updateAppointmentTimeSlot(startTimeFormatted, formatted)
                    showEndTimePicker = false
                }) { Text("Select") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
fun FormLabel(text: String) {
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
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFEEEEEE),
                unfocusedContainerColor = Color(0xFFEEEEEE),
                disabledContainerColor = Color(0xFFEEEEEE),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
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
private fun AnimatedErrorMessage(visible: Boolean, message: String) {
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
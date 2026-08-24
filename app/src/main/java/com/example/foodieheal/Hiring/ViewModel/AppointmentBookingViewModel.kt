package com.example.foodieheal.hiring.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.model.AppointmentUiState
import com.example.foodieheal.hiring.model.AppointmentValidationError
import com.example.foodieheal.hiring.model.ChefAppointmentsUiState
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.model.Appointment
import com.example.mobileassignmentloginpart.Model.Chef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppointmentBookingViewModel(
    private val repository: HiringRepository = HiringRepository(),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _selectedChef = MutableStateFlow<Chef?>(null)
    val selectedChef: StateFlow<Chef?> = _selectedChef.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()

    private val _chefAppointmentsState = MutableStateFlow(ChefAppointmentsUiState())
    val chefAppointmentsState: StateFlow<ChefAppointmentsUiState> = _chefAppointmentsState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    val currentChefId: String
        get() = selectedChef.value?.let { it.chefId.ifEmpty { it.id } }.orEmpty()

    init {
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.isConnected.collect { connected ->
                    _isNetworkAvailable.value = connected
                    if (connected && currentChefId.isNotBlank()) {
                        fetchAppointmentsForChef(currentChefId)
                    }
                }
            }
        }
    }

    fun selectChef(chef: Chef) {
        _selectedChef.value = chef
    }

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun clearSelectedChef() {
        _selectedChef.value = null
    }

    fun onAppointmentTimeChanged(time: String) {
        _uiState.update { it.copy(appointmentTime = time) }
        validateTimeSlot(time)
        revalidateIfSubmitted()
    }

    fun onAddressChanged(address: String) {
        _uiState.update { it.copy(address = address) }
        revalidateIfSubmitted()
    }

    fun onPostcodeChanged(postcode: String) {
        if (postcode.all { it.isDigit() }) {
            _uiState.update { it.copy(postcode = postcode) }
            revalidateIfSubmitted()
        }
    }

    fun onStateChanged(state: String) {
        _uiState.update { it.copy(state = state) }
        revalidateIfSubmitted()
    }

    fun onHealthPreferenceChanged(healthPreference: String) {
        _uiState.update { it.copy(healthPreference = healthPreference) }
        revalidateIfSubmitted()
    }

    fun onServingSizeChanged(servingSize: String) {
        if (servingSize.all { it.isDigit() }) {
            _uiState.update { it.copy(servingSize = servingSize) }
            revalidateIfSubmitted()
        }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
        revalidateIfSubmitted()
    }

    fun clearAppointmentForm() {
        _uiState.value = AppointmentUiState()
    }

    private fun revalidateIfSubmitted() {
        if (_uiState.value.hasAttemptedSubmit) {
            val errors = validateForm()
            _uiState.update { it.copy(errors = errors) }
        }
    }

    fun validateAndSubmit(onValid: () -> Unit) {
        _uiState.update { it.copy(hasAttemptedSubmit = true) }
        val errors = validateForm()
        _uiState.update { it.copy(errors = errors) }
        if (errors.isEmpty()) {
            onValid()
        }
    }

    fun fetchAppointmentsForChef(chefId: String) {
        if (chefId.isBlank()) return

        viewModelScope.launch {
            _chefAppointmentsState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (appointments, chefUser) = repository.fetchChefAppointments(chefId)
                _chefAppointmentsState.update {
                    it.copy(
                        appointments = appointments,
                        chefUser = chefUser,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error fetching chef schedule", e)
                _chefAppointmentsState.update {
                    it.copy(
                        errorMessage = e.localizedMessage ?: "Failed to fetch chef schedule",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun validateForm(): Set<AppointmentValidationError> {
        val state = uiState.value
        return validateFormValues(
            appointmentTime = state.appointmentTime,
            address = state.address,
            postcode = state.postcode,
            state = state.state,
            servingSize = state.servingSize,
            description = state.description,
            targetDate = selectedDate.value.toString()
        )
    }

    fun validateFormValues(
        appointmentTime: String,
        address: String,
        postcode: String,
        state: String,
        servingSize: String,
        description: String,
        targetDate: String,
        currentAppointmentId: String? = null
    ): Set<AppointmentValidationError> {
        val errors = mutableSetOf<AppointmentValidationError>()

        if (appointmentTime.isBlank() || !appointmentTime.contains("-")) {
            errors.add(AppointmentValidationError.InvalidTime)
        } else {
            val parts = appointmentTime.split("-").map { it.trim() }
            if (parts.size == 2) {
                val parsedTimes = parseTimeSlot(parts[0], parts[1])
                if (parsedTimes == null) {
                    errors.add(AppointmentValidationError.InvalidTime)
                } else {
                    val (startCal, endCal) = parsedTimes
                    if (!endCal.after(startCal)) {
                        errors.add(AppointmentValidationError.InvalidTime)
                    } else if (isSlotOverlapping(startCal, endCal, targetDate, currentAppointmentId)) {
                        errors.add(AppointmentValidationError.TimeSlotOccupied)
                    }
                }
            } else {
                errors.add(AppointmentValidationError.InvalidTime)
            }
        }

        if (address.isBlank()) errors.add(AppointmentValidationError.InvalidAddress)
        if (postcode.isBlank() || postcode.length != 5) {
            errors.add(AppointmentValidationError.InvalidPostcode)
        }
        if (state.isBlank()) errors.add(AppointmentValidationError.InvalidState)
        if (servingSize.isBlank() || servingSize.toIntOrNull() == null || (servingSize.toIntOrNull() ?: 0) <= 0) {
            errors.add(AppointmentValidationError.InvalidServingSize)
        }
        if (description.isBlank()) errors.add(AppointmentValidationError.InvalidDescription)

        return errors
    }

    fun validateTimeSlot(time: String) {
        if (!time.contains("-")) return
        val parts = time.split("-").map { it.trim() }
        if (parts.size != 2) return

        val parsedTimes = parseTimeSlot(parts[0], parts[1]) ?: return
        val (startCal, endCal) = parsedTimes

        val targetDate = selectedDate.value.toString()
        val isOccupied = isSlotOverlapping(startCal, endCal, targetDate)

        _uiState.update { currentState ->
            val updatedErrors = currentState.errors.toMutableSet()
            if (isOccupied) {
                updatedErrors.add(AppointmentValidationError.TimeSlotOccupied)
            } else {
                updatedErrors.remove(AppointmentValidationError.TimeSlotOccupied)
            }
            currentState.copy(errors = updatedErrors, isTimeSlotOccupied = isOccupied)
        }
    }

    private fun parseTime(timeStr: String): Date? {
        val trimmed = timeStr.trim()
        val patterns = listOf("hh:mm a", "h:mm a", "HH:mm:ss", "HH:mm", "H:mm:ss", "H:mm")
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                val date = format.parse(trimmed)
                if (date != null) return date
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseTimeSlot(startTimeStr: String, endTimeStr: String): Pair<Calendar, Calendar>? {
        return try {
            val startDate = parseTime(startTimeStr) ?: return null
            val endDate = parseTime(endTimeStr) ?: return null

            val startCal = Calendar.getInstance().apply { time = startDate }
            val endCal = Calendar.getInstance().apply { time = endDate }

            Pair(startCal, endCal)
        } catch (e: Exception) {
            null
        }
    }

    private fun isSlotOverlapping(
        startCal: Calendar,
        endCal: Calendar,
        targetDate: String,
        currentAppointmentId: String? = null
    ): Boolean {
        val appointments = chefAppointmentsState.value.appointments.filter {
            it.Date == targetDate &&
                    !it.Status.equals("cancelled", ignoreCase = true) &&
                    !it.Status.equals("rejected", ignoreCase = true) &&
                    (currentAppointmentId == null || it.AppointmentID != currentAppointmentId)
        }

        for (appt in appointments) {
            try {
                val apptStart = parseTime(appt.Start_Time) ?: continue
                val apptEnd = parseTime(appt.End_Time) ?: continue

                val apptStartCal = Calendar.getInstance().apply { time = apptStart }
                val apptEndCal = Calendar.getInstance().apply { time = apptEnd }

                // Overlap occurs if new Start < existing End AND new End > existing Start
                val isOverlap = startCal.before(apptEndCal) && endCal.after(apptStartCal)
                if (isOverlap) return true
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error parsing appointment times for overlap check", e)
            }
        }
        return false
    }

    fun calculateTotalPrice(hourlyRate: Double, appointmentTime: String): Double {
        if (!appointmentTime.contains("-")) return hourlyRate
        val parts = appointmentTime.split("-").map { it.trim() }
        if (parts.size != 2) return hourlyRate

        return try {
            val start = parseTime(parts[0])
            val end = parseTime(parts[1])
            if (start != null && end != null) {
                val diffMillis = end.time - start.time
                val diffHours = diffMillis.toDouble() / (1000 * 60 * 60)
                val actualHours = if (diffHours > 0) diffHours else 1.0
                hourlyRate * actualHours
            } else {
                hourlyRate
            }
        } catch (e: Exception) {
            hourlyRate
        }
    }

    fun createAppointment(
        userId: String,
        chefId: String,
        selectedDate: String,
        startTime: String,
        endTime: String,
        totalPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!_isNetworkAvailable.value) {
            onError("No internet connection. Please connect to the internet to complete booking.")
            return
        }

        if (userId.isBlank() || chefId.isBlank()) {
            onError("Invalid user or chef ID.")
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        val state = uiState.value
        val newAppointment = Appointment(
            Date = selectedDate,
            Start_Time = startTime,
            End_Time = endTime,
            Address = state.address.trim(),
            Postcode = state.postcode.trim(),
            State = state.state,
            Note = state.description.trim(),
            Serving_Size = state.servingSize.toIntOrNull() ?: 1,
            Health_Preference = state.healthPreference,
            Total_Price = totalPrice,
            Status = "Pending",
            chefId = chefId,
            userId = userId
        )

        viewModelScope.launch {
            try {
                repository.createAppointment(newAppointment)
                _uiState.update { it.copy(isSubmitting = false) }
                clearAppointmentForm()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false) }
                onError(e.localizedMessage ?: "An error occurred while creating appointment")
            }
        }
    }
}

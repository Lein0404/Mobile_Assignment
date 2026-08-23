package com.example.foodieheal.hiring.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.model.AppointmentUiState
import com.example.foodieheal.hiring.model.AppointmentValidationError
import com.example.foodieheal.hiring.model.ChefAppointmentsUiState
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
import java.util.Locale

class AppointmentBookingViewModel(
    private val repository: HiringRepository = HiringRepository()
) : ViewModel() {

    private val _selectedChef = MutableStateFlow<Chef?>(null)
    val selectedChef: StateFlow<Chef?> = _selectedChef.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()

    private val _chefAppointmentsState = MutableStateFlow(ChefAppointmentsUiState())
    val chefAppointmentsState: StateFlow<ChefAppointmentsUiState> = _chefAppointmentsState.asStateFlow()

    val currentChefId: String
        get() = selectedChef.value?.let { it.chefId.ifEmpty { it.id } }.orEmpty()

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
        _uiState.update { it.copy(postcode = postcode) }
        revalidateIfSubmitted()
    }

    fun onStateChanged(state: String) {
        _uiState.update { it.copy(state = state) }
        revalidateIfSubmitted()
    }

    fun onServingSizeChanged(servingSize: String) {
        _uiState.update { it.copy(servingSize = servingSize) }
        revalidateIfSubmitted()
    }

    fun onHealthPreferenceChanged(healthPreference: String) {
        _uiState.update { it.copy(healthPreference = healthPreference) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
        revalidateIfSubmitted()
    }

    fun clearAppointmentForm() {
        _uiState.value = AppointmentUiState()
    }

    fun fetchAppointmentsForChef(chefId: String) {
        if (chefId.isBlank()) return
        viewModelScope.launch {
            _chefAppointmentsState.update { it.copy(isLoading = true) }
            try {
                val (appointments, chefUser) = repository.fetchChefAppointments(chefId)
                _chefAppointmentsState.value = ChefAppointmentsUiState(
                    isLoading = false,
                    appointments = appointments,
                    chefUser = chefUser
                )
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error fetching appointments for chef $chefId", e)
                _chefAppointmentsState.value = ChefAppointmentsUiState(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load details"
                )
            }
        }
    }

    private fun validateTimeSlot(timeSlot: String) {
        val isOccupied = checkTimeSlotOverlap(timeSlot)
        val currentErrors = _uiState.value.errors.toMutableSet()

        if (timeSlot.isBlank()) {
            currentErrors.add(AppointmentValidationError.InvalidTime)
            currentErrors.remove(AppointmentValidationError.TimeSlotOccupied)
        } else if (isOccupied) {
            currentErrors.remove(AppointmentValidationError.InvalidTime)
            currentErrors.add(AppointmentValidationError.TimeSlotOccupied)
        } else {
            currentErrors.remove(AppointmentValidationError.InvalidTime)
            currentErrors.remove(AppointmentValidationError.TimeSlotOccupied)
        }

        _uiState.update {
            it.copy(
                isTimeSlotOccupied = isOccupied,
                errors = currentErrors
            )
        }
    }

    fun checkTimeSlotOverlap(
        timeSlot: String,
        targetDateStr: String? = null,
        currentAppointmentId: String? = null
    ): Boolean {
        if (!timeSlot.contains(" - ")) return false
        val parts = timeSlot.split(" - ")
        if (parts.size != 2) return false

        val newStart = parseTimeToMinutes(parts[0])
        val newEnd = parseTimeToMinutes(parts[1])

        if (newStart == null || newEnd == null || newEnd <= newStart) return false

        val existingAppointments = _chefAppointmentsState.value.appointments
        val targetDate = targetDateStr?.trim() ?: selectedDate.value.toString()

        return existingAppointments.any { appt ->
            if (currentAppointmentId != null && appt.AppointmentID == currentAppointmentId) return@any false

            val status = appt.Status?.lowercase(Locale.US).orEmpty()
            val isActive = status !in listOf("cancelled", "rejected", "completed")

            val apptDate = appt.Date?.trim().orEmpty()
            val isSameDate = apptDate.equals(targetDate, ignoreCase = true) ||
                    apptDate.contains(targetDate) ||
                    targetDate.contains(apptDate)

            val existingStart = parseTimeToMinutes(appt.Start_Time)
            val existingEnd = parseTimeToMinutes(appt.End_Time)

            if (isActive && isSameDate && existingStart != null && existingEnd != null) {
                (newStart < existingEnd) && (newEnd > existingStart)
            } else {
                false
            }
        }
    }

    private fun parseTimeToMinutes(timeStr: String?): Int? {
        if (timeStr.isNullOrBlank()) return null
        val cleanStr = timeStr.trim().uppercase(Locale.US)
        val formats = listOf("hh:mm a", "h:mm a", "HH:mm:ss", "HH:mm", "yyyy-MM-dd'T'HH:mm:ss")

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                val date = sdf.parse(cleanStr)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }
            } catch (_: Exception) { }
        }
        return null
    }

    fun validateFormValues(
        appointmentTime: String,
        address: String,
        postcode: String,
        state: String,
        servingSize: String,
        description: String,
        targetDate: String? = null,
        currentAppointmentId: String? = null
    ): Set<AppointmentValidationError> {
        val errors = mutableSetOf<AppointmentValidationError>()

        if (appointmentTime.isBlank()) {
            errors.add(AppointmentValidationError.InvalidTime)
        } else if (checkTimeSlotOverlap(appointmentTime, targetDate, currentAppointmentId)) {
            errors.add(AppointmentValidationError.TimeSlotOccupied)
        }

        if (address.isBlank()) errors.add(AppointmentValidationError.InvalidAddress)
        if (!postcode.matches(Regex("^[0-9]{5}$"))) errors.add(AppointmentValidationError.InvalidPostcode)
        if (state.isBlank()) errors.add(AppointmentValidationError.InvalidState)
        if ((servingSize.toIntOrNull() ?: 0) <= 0) errors.add(AppointmentValidationError.InvalidServingSize)
        if (description.trim().isBlank()) errors.add(AppointmentValidationError.InvalidDescription)

        return errors
    }

    fun validateAndSubmit(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val newErrors = validateFormValues(
            appointmentTime = currentState.appointmentTime,
            address = currentState.address,
            postcode = currentState.postcode,
            state = currentState.state,
            servingSize = currentState.servingSize,
            description = currentState.description
        )

        _uiState.update {
            it.copy(
                hasAttemptedSubmit = true,
                errors = newErrors,
                isTimeSlotOccupied = newErrors.contains(AppointmentValidationError.TimeSlotOccupied)
            )
        }

        if (newErrors.isEmpty()) onSuccess()
    }

    private fun revalidateIfSubmitted() {
        if (_uiState.value.hasAttemptedSubmit) {
            validateAndSubmit(onSuccess = {})
        }
    }

    fun calculateTotalPrice(): Double {
        val hourlyRate = selectedChef.value?.Pricing ?: 0.0
        val timeString = uiState.value.appointmentTime

        if (!timeString.contains(" - ")) return hourlyRate
        val parts = timeString.split(" - ")
        if (parts.size != 2) return hourlyRate

        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        return try {
            val startDate = sdf.parse(parts[0].trim())
            val endDate = sdf.parse(parts[1].trim())

            if (startDate != null && endDate != null) {
                val diffInMillis = endDate.time - startDate.time
                var hours = diffInMillis.toDouble() / (1000 * 60 * 60)

                if (hours < 0) {
                    hours += 24.0
                }

                val actualHours = if (hours > 0) hours else 1.0
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

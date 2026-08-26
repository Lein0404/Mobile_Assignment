package com.example.foodieheal.hiring.model

import com.example.foodieheal.R

sealed interface AppointmentValidationError {
    data object InvalidTime : AppointmentValidationError
    data object TimeSlotOccupied : AppointmentValidationError
    data object InvalidAddress : AppointmentValidationError
    data object InvalidPostcode : AppointmentValidationError
    data object InvalidState : AppointmentValidationError
    data object InvalidServingSize : AppointmentValidationError
    data object InvalidDescription : AppointmentValidationError
}

data class AppointmentUiState(
    val appointmentTime: String = "09:00 AM - 11:00 AM",
    val startTime: String = "09:00 AM",
    val endTime: String = "11:00 AM",
    val isTimeSlotOccupied: Boolean = false,
    val address: String = "",
    val postcode: String = "",
    val state: String = "",
    val servingSize: String = "",
    val healthPreference: String = "",
    val description: String = "",
    val errors: Set<AppointmentValidationError> = emptySet(),
    val hasAttemptedSubmit: Boolean = false,
    val isSubmitting: Boolean = false
) {
    val isTimeValid: Boolean get() = appointmentTime.isNotBlank() && !isTimeSlotOccupied
    val isAddressValid: Boolean get() = address.trim().isNotBlank()
    val isPostcodeValid: Boolean get() = postcode.matches(Regex("^[0-9]{5}$"))
    val isStateValid: Boolean get() = state.isNotBlank()
    val isServingSizeValid: Boolean get() = servingSize.toIntOrNull()?.let { it > 0 } == true
    val isDescriptionValid: Boolean get() = description.trim().isNotBlank()

    val canSubmit: Boolean
        get() = isTimeValid &&
                isAddressValid &&
                isPostcodeValid &&
                isStateValid &&
                isServingSizeValid &&
                isDescriptionValid

    val timeErrorRes: Int?
        get() = when {
            !hasAttemptedSubmit -> null
            errors.contains(AppointmentValidationError.TimeSlotOccupied) -> R.string.error_time_slot_occupied
            errors.contains(AppointmentValidationError.InvalidTime) -> R.string.error_invalid_time_range
            else -> null
        }

    val addressErrorRes: Int?
        get() = if (hasAttemptedSubmit && errors.contains(AppointmentValidationError.InvalidAddress)) {
            R.string.error_empty_address
        } else null

    val postcodeErrorRes: Int?
        get() = if (hasAttemptedSubmit && errors.contains(AppointmentValidationError.InvalidPostcode)) {
            R.string.error_invalid_postcode
        } else null

    val stateErrorRes: Int?
        get() = if (hasAttemptedSubmit && errors.contains(AppointmentValidationError.InvalidState)) {
            R.string.error_select_state
        } else null

    val servingSizeErrorRes: Int?
        get() = if (hasAttemptedSubmit && errors.contains(AppointmentValidationError.InvalidServingSize)) {
            R.string.error_invalid_serving_size
        } else null

    val descriptionErrorRes: Int?
        get() = if (hasAttemptedSubmit && errors.contains(AppointmentValidationError.InvalidDescription)) {
            R.string.error_empty_description
        } else null
}

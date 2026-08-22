package com.example.foodieheal.hiring.model

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
    val appointmentTime: String = "",
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
    val isTimeValid: Boolean get() = appointmentTime.isNotBlank()
    val isAddressValid: Boolean get() = address.isNotBlank()
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
}

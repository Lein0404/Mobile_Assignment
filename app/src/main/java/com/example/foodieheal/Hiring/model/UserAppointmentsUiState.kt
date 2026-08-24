package com.example.foodieheal.hiring.model

import com.example.foodieheal.model.Appointment
import com.example.foodieheal.model.User

sealed interface UserAppointmentsUiState {
    data object Loading : UserAppointmentsUiState
    data class Success(
        val appointments: List<Appointment>,
        val usersMap: Map<String, User> = emptyMap()
    ) : UserAppointmentsUiState
    data class Error(val message: String) : UserAppointmentsUiState
}

package com.example.foodieheal.hiring.model

import com.example.foodieheal.User.Model.User
import com.example.foodieheal.model.Appointment


sealed interface UserAppointmentsUiState {
    data object Loading : UserAppointmentsUiState
    data class Success(
        val appointments: List<Appointment>,
        val usersMap: Map<String, User> = emptyMap()
    ) : UserAppointmentsUiState
    data class Error(val message: String) : UserAppointmentsUiState
}

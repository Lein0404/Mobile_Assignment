package com.example.foodieheal.hiring.model

import com.example.foodieheal.User.Model.User
import com.example.foodieheal.model.Appointment


data class ChefAppointmentsUiState(
    val isLoading: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val chefUser: User? = null,
    val errorMessage: String? = null
)

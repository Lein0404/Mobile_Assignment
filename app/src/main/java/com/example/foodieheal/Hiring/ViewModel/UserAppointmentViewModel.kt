package com.example.foodieheal.hiring.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.model.UserAppointmentsUiState
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserAppointmentViewModel(
    private val repository: HiringRepository = HiringRepository(),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _userAppointmentsState = MutableStateFlow<UserAppointmentsUiState>(UserAppointmentsUiState.Loading)
    val userAppointmentsState: StateFlow<UserAppointmentsUiState> = _userAppointmentsState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    init {
        fetchAppointmentsForCurrentUser()
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.isConnected.collect { connected ->
                    _isNetworkAvailable.value = connected
                    if (connected) {
                        fetchAppointmentsForCurrentUser()
                    }
                }
            }
        }
    }

    fun fetchAppointmentsForCurrentUser() {
        viewModelScope.launch {
            _userAppointmentsState.value = UserAppointmentsUiState.Loading
            try {
                val currentUserId = repository.getCurrentUserId()
                if (currentUserId.isNullOrEmpty()) {
                    _userAppointmentsState.value = UserAppointmentsUiState.Error("User not logged in.")
                    return@launch
                }

                val appointments = repository.fetchAppointmentsForUser(currentUserId)
                val chefsMap = repository.fetchChefsMapForAppointments(appointments)

                _userAppointmentsState.value = UserAppointmentsUiState.Success(
                    appointments = appointments,
                    usersMap = chefsMap
                )
            } catch (e: Exception) {
                Log.e("UserAppointmentVM", "Error fetching user appointments", e)
                _userAppointmentsState.value = UserAppointmentsUiState.Error(
                    e.localizedMessage ?: "Failed to load appointments"
                )
            }
        }
    }

    fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateAppointmentStatus(appointmentId, newStatus)
                fetchAppointmentsForCurrentUser()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to update appointment status")
            }
        }
    }

    fun cancelAppointment(
        appointmentId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateAppointmentStatus(
            appointmentId = appointmentId,
            newStatus = "Cancelled",
            onSuccess = onSuccess,
            onError = { rawError ->
                val errorMessage = "Failed to cancel appointment: $rawError"
                onError(errorMessage)
            }
        )
    }

    fun rescheduleAppointment(
        appointmentId: String,
        newDate: String,
        newStartTime: String,
        newEndTime: String,
        newAddress: String,
        newPostcode: String,
        newState: String,
        newServingSize: Int,
        newDescription: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.rescheduleAppointment(
                    appointmentId, newDate, newStartTime, newEndTime,
                    newAddress, newPostcode, newState, newServingSize, newDescription
                )
                fetchAppointmentsForCurrentUser()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to reschedule appointment")
            }
        }
    }
}

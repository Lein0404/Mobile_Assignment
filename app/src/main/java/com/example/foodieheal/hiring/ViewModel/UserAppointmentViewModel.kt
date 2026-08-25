package com.example.foodieheal.hiring.viewmodel

import android.content.Context
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

    private val _deletedAppointmentIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedAppointmentIds: StateFlow<Set<String>> = _deletedAppointmentIds.asStateFlow()

    private var hasLoadedSuccessfully = false

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
                        fetchAppointmentsForCurrentUser(forceRefresh = false)
                    }
                }
            }
        }
    }

    fun fetchAppointmentsForCurrentUser(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Only show full-screen loading if we don't have cached success data or user explicitly forces refresh
            if (!hasLoadedSuccessfully || forceRefresh) {
                _userAppointmentsState.value = UserAppointmentsUiState.Loading
            }

            try {
                val currentUserId = repository.getCurrentUserId()
                if (currentUserId.isNullOrEmpty()) {
                    _userAppointmentsState.value = UserAppointmentsUiState.Error("User not logged in.")
                    return@launch
                }

                val appointments = repository.fetchAppointmentsForUser(currentUserId)
                val chefsMap = repository.fetchChefsMapForAppointments(appointments)

                hasLoadedSuccessfully = true
                _userAppointmentsState.value = UserAppointmentsUiState.Success(
                    appointments = appointments,
                    usersMap = chefsMap
                )
            } catch (e: Exception) {
                Log.e("UserAppointmentVM", "Error fetching user appointments", e)
                // If we already have data, don't wipe it out on background fetch failure
                if (!hasLoadedSuccessfully) {
                    _userAppointmentsState.value = UserAppointmentsUiState.Error(
                        e.localizedMessage ?: "Failed to load appointments"
                    )
                }
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
                fetchAppointmentsForCurrentUser(forceRefresh = false)
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
        viewModelScope.launch {
            try {
                repository.cancelAppointment(appointmentId)
                fetchAppointmentsForCurrentUser(forceRefresh = true)
                onSuccess()
            } catch (e: Exception) {
                val errorMessage = "Failed to cancel appointment: ${e.localizedMessage}"
                onError(errorMessage)
            }
        }
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
        newTotalPrice: Double = 0.0,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Reschedules appointment: refunds previous payment (if confirmed/paid) and resets status to Pending
                // TODO: In the future, integrate with digital wallet feature to credit refunded amount back to user's wallet balance
                repository.rescheduleAppointment(
                    appointmentId, newDate, newStartTime, newEndTime,
                    newAddress, newPostcode, newState, newServingSize, newDescription, newTotalPrice
                )
                fetchAppointmentsForCurrentUser(forceRefresh = true)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to reschedule appointment")
            }
        }
    }

    fun loadDeletedAppointments(context: Context) {
        val currentUserId = repository.getCurrentUserId() ?: "default"
        val prefs = context.getSharedPreferences("user_appointment_history_${currentUserId}", Context.MODE_PRIVATE)
        val currentDeleted = prefs.getStringSet("deleted_appointment_ids", emptySet()) ?: emptySet()
        _deletedAppointmentIds.value = currentDeleted
    }

    fun softDeleteAppointment(context: Context, appointmentId: String) {
        val currentUserId = repository.getCurrentUserId() ?: "default"
        val prefs = context.getSharedPreferences("user_appointment_history_${currentUserId}", Context.MODE_PRIVATE)
        val currentDeleted = prefs.getStringSet("deleted_appointment_ids", emptySet()) ?: emptySet()
        val updatedDeleted = currentDeleted + appointmentId
        prefs.edit().putStringSet("deleted_appointment_ids", updatedDeleted).apply()
        _deletedAppointmentIds.value = updatedDeleted
    }

    fun restoreAppointment(context: Context, appointmentId: String) {
        val currentUserId = repository.getCurrentUserId() ?: "default"
        val prefs = context.getSharedPreferences("user_appointment_history_${currentUserId}", Context.MODE_PRIVATE)
        val currentDeleted = prefs.getStringSet("deleted_appointment_ids", emptySet()) ?: emptySet()
        val updatedDeleted = currentDeleted - appointmentId
        prefs.edit().putStringSet("deleted_appointment_ids", updatedDeleted).apply()
        _deletedAppointmentIds.value = updatedDeleted
    }
}

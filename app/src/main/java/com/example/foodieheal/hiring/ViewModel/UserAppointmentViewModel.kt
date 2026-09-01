package com.example.foodieheal.hiring.viewmodel

import android.content.Context
import android.util.Log
import com.example.foodieheal.R
import com.example.foodieheal.MainActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.model.UserAppointmentsUiState
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.hiring.model.AppointmentRecipeWithDetails
import com.example.foodieheal.hiring.model.SelectedAppointmentRecipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.foodieheal.hiring.util.HiringNetworkHelper

class UserAppointmentViewModel(
    private val repository: HiringRepository = HiringRepository(),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private fun resString(resId: Int, vararg args: Any): String? {
        return MainActivity.appContext?.getString(resId, *args)
    }

    private val _userAppointmentsState = MutableStateFlow<UserAppointmentsUiState>(UserAppointmentsUiState.Loading)
    val userAppointmentsState: StateFlow<UserAppointmentsUiState> = _userAppointmentsState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(HiringNetworkHelper.isDeviceOnline())
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _deletedAppointmentIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedAppointmentIds: StateFlow<Set<String>> = _deletedAppointmentIds.asStateFlow()

    // Attached recipes map keyed by appointmentId
    private val _attachedRecipes = MutableStateFlow<Map<String, List<AppointmentRecipeWithDetails>>>(emptyMap())
    val attachedRecipes: StateFlow<Map<String, List<AppointmentRecipeWithDetails>>> = _attachedRecipes.asStateFlow()

    private val _isLoadingRecipes = MutableStateFlow(false)
    val isLoadingRecipes: StateFlow<Boolean> = _isLoadingRecipes.asStateFlow()

    private var hasLoadedSuccessfully = false

    fun loadRecipesForAppointment(appointmentId: String) {
        if (appointmentId.isBlank()) return
        viewModelScope.launch {
            _isLoadingRecipes.value = true
            try {
                val recipes = repository.fetchAppointmentRecipes(appointmentId)
                _attachedRecipes.update { it + (appointmentId to recipes) }
            } catch (e: Exception) {
                Log.e("UserAppointmentVM", "Error loading recipes for appointment $appointmentId", e)
            } finally {
                _isLoadingRecipes.value = false
            }
        }
    }

    suspend fun fetchAppointmentRecipes(appointmentId: String): List<AppointmentRecipeWithDetails> {
        return repository.fetchAppointmentRecipes(appointmentId)
    }

    init {
        checkPhoneCacheStatus()
        fetchAppointmentsForCurrentUser()
        observeNetworkStatus()
    }

    private fun checkPhoneCacheStatus() {
        viewModelScope.launch {
            com.example.foodieheal.MainActivity.appContext?.let { ctx ->
                repository.checkAndClearCacheIfPhoneCacheCleared(ctx)
            }
        }
    }

    private fun observeNetworkStatus() {
        HiringNetworkHelper.observeHiringNetwork(
            networkMonitor = networkMonitor,
            coroutineScope = viewModelScope,
            stateFlow = _isNetworkAvailable,
            onReconnected = { fetchAppointmentsForCurrentUser(forceRefresh = false) }
        )
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
                    _userAppointmentsState.value = UserAppointmentsUiState.Error(
                        resString(R.string.error_user_not_logged_in) ?: "User not logged in."
                    )
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
                        e.localizedMessage ?: (resString(R.string.error_failed_to_load_appointments) ?: "Failed to load appointments")
                    )
                }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearAppointmentCache()
            _userAppointmentsState.value = UserAppointmentsUiState.Loading
            hasLoadedSuccessfully = false
            fetchAppointmentsForCurrentUser(forceRefresh = true)
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
                onError(e.localizedMessage ?: (resString(R.string.error_failed_update_appointment_status) ?: "Failed to update appointment status"))
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
                val detail = e.localizedMessage ?: ""
                val errorMessage = resString(R.string.error_failed_cancel_appointment, detail) ?: "Failed to cancel appointment: $detail"
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
        newRecipes: List<SelectedAppointmentRecipe>? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Reschedules appointment: credits previous payment (if confirmed/paid) back to user's wallet and resets status to Pending
                repository.rescheduleAppointment(
                    appointmentId, newDate, newStartTime, newEndTime,
                    newAddress, newPostcode, newState, newServingSize, newDescription, newTotalPrice,
                    newRecipes
                )
                loadRecipesForAppointment(appointmentId)
                fetchAppointmentsForCurrentUser(forceRefresh = true)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: (resString(R.string.error_failed_reschedule_appointment) ?: "Failed to reschedule appointment"))
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

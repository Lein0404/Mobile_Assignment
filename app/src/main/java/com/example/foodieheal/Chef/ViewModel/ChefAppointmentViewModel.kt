package com.example.foodieheal.Chef.ViewModel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Chef.data.ChefPortalRepository
import com.example.foodieheal.Chef.local.ChefDatabase
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.hiring.model.AppointmentRecipeWithDetails
import com.example.foodieheal.User.Model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val totalCount: Int,
        val nextAppointment: Appointment?,
        val usersMap: Map<String, User> = emptyMap(),
        val allAppointments: List<Appointment> = emptyList()
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

sealed interface AppointmentsUiState {
    object Loading : AppointmentsUiState
    data class Success(
        val appointments: List<Appointment>,
        val usersMap: Map<String, User> = emptyMap()
    ) : AppointmentsUiState
    data class Error(
        val message: String
    ) : AppointmentsUiState
}

class ChefPortalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChefDatabase.getInstance(application)
    private val repository = ChefPortalRepository(
        database.chefPortalAppointmentDao(),
        database.chefPortalUserDao(),
        database.chefProfileDao()
    )

    private val networkMonitor = NetworkMonitor(application)

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    // Appointments screen state flow
    private val _appointmentsUiState = MutableStateFlow<AppointmentsUiState>(AppointmentsUiState.Loading)
    val appointmentsUiState: StateFlow<AppointmentsUiState> = _appointmentsUiState.asStateFlow()

    // Home screen state flow
    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    // Attached recipes map keyed by appointmentId
    private val _attachedRecipes = MutableStateFlow<Map<String, List<AppointmentRecipeWithDetails>>>(emptyMap())
    val attachedRecipes: StateFlow<Map<String, List<AppointmentRecipeWithDetails>>> = _attachedRecipes.asStateFlow()

    private val _isLoadingRecipes = MutableStateFlow(false)
    val isLoadingRecipes: StateFlow<Boolean> = _isLoadingRecipes.asStateFlow()

    var selectedAppointment by mutableStateOf<Appointment?>(null)
        private set

    fun loadRecipesForAppointment(appointmentId: String) {
        if (appointmentId.isBlank()) return
        viewModelScope.launch {
            _isLoadingRecipes.value = true
            try {
                val recipes = repository.fetchAppointmentRecipes(appointmentId)
                _attachedRecipes.update { it + (appointmentId to recipes) }
            } catch (e: Exception) {
                Log.e("ChefPortalVM", "Error loading recipes for appointment $appointmentId", e)
            } finally {
                _isLoadingRecipes.value = false
            }
        }
    }

    fun selectAppointment(appointment: Appointment?) {
        selectedAppointment = appointment
        appointment?.AppointmentID?.let { id ->
            if (id.isNotBlank()) {
                loadRecipesForAppointment(id)
            }
        }
    }

    init {
        observeNetworkStatus()
        fetchAppointmentsForCurrentChef()
        loadDashboardData()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _isNetworkAvailable.value = connected
                if (connected) {
                    // Reconnected: sync fresh appointments and dashboard stats
                    fetchAppointmentsForCurrentChef()
                    loadDashboardData()
                }
            }
        }
    }

    // Call this for AppointmentsScreen
    fun fetchAppointmentsForCurrentChef() {
        viewModelScope.launch {
            _appointmentsUiState.value = AppointmentsUiState.Loading
            try {
                val currentUserId = repository.getCurrentUserId()

                if (currentUserId.isNullOrEmpty()) {
                    _appointmentsUiState.value = AppointmentsUiState.Error("User not logged in.")
                    return@launch
                }

                val appointments = repository.fetchAppointmentsForChef(currentUserId)

                // Batch fetch associated users
                val usersMap = repository.fetchUsersForAppointments(appointments)

                _appointmentsUiState.value = AppointmentsUiState.Success(
                    appointments = appointments,
                    usersMap = usersMap
                )

            } catch (e: Exception) {
                Log.e("AppointmentsVM", "Error fetching appointments", e)
                _appointmentsUiState.value = AppointmentsUiState.Error(
                    e.localizedMessage ?: "Failed to load appointments"
                )
            }
        }
    }

    // Call this for ChefHomeScreen
    fun loadDashboardData() {
        viewModelScope.launch {
            _homeUiState.value = HomeUiState.Loading
            try {
                val currentUserId = repository.getCurrentUserId()

                if (currentUserId.isNullOrEmpty()) {
                    _homeUiState.value = HomeUiState.Error("User not logged in")
                    return@launch
                }

                val appointments = repository.fetchAppointmentsForChef(currentUserId)

                val activeAppointments = appointments.filter {
                    it.Status.lowercase() != "cancelled" &&
                            it.Status.lowercase() != "completed" &&
                            it.Status.lowercase() != "rejected"
                }

                val nextApp = activeAppointments.firstOrNull()

                // Batch fetch user details (for the next appointment card)
                val usersMap = repository.fetchUsersForAppointments(listOfNotNull(nextApp))

                _homeUiState.value = HomeUiState.Success(
                    totalCount = activeAppointments.size,
                    nextAppointment = nextApp,
                    usersMap = usersMap,
                    allAppointments = appointments
                )

            } catch (e: Exception) {
                Log.e("ChefHomeVM", "Error loading home dashboard", e)
                _homeUiState.value = HomeUiState.Error(
                    e.localizedMessage ?: "Failed to load dashboard"
                )
            }
        }
    }

    fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        rejectionReason: String? = null
    ) {
        viewModelScope.launch {
            if (!_isNetworkAvailable.value) {
                _uiEvent.emit("Cannot update appointment status while offline.")
                return@launch
            }

            //Snapshot current state for rollback
            val previousAppointmentsState = _appointmentsUiState.value
            val previousHomeState = _homeUiState.value

            fun applyOptimisticUpdate(appointments: List<Appointment>): List<Appointment> =
                appointments.map { appt ->
                    if (appt.AppointmentID == appointmentId) {
                        appt.copy(
                            Status = newStatus,
                            Reject_Reason = if (newStatus == "Rejected") rejectionReason else appt.Reject_Reason
                        )
                    } else appt
                }

            // Update Appointments screen state
            if (previousAppointmentsState is AppointmentsUiState.Success) {
                _appointmentsUiState.update { current ->
                    if (current is AppointmentsUiState.Success) {
                        current.copy(appointments = applyOptimisticUpdate(current.appointments))
                    } else current
                }
            }

            // Update Home dashboard state (active count + allAppointments)
            if (previousHomeState is HomeUiState.Success) {
                _homeUiState.update { current ->
                    if (current is HomeUiState.Success) {
                        val updatedAll = applyOptimisticUpdate(current.allAppointments)
                        val updatedActive = updatedAll.filter {
                            val s = it.Status.lowercase()
                            s != "cancelled" && s != "completed" && s != "rejected"
                        }
                        current.copy(
                            allAppointments  = updatedAll,
                            totalCount       = updatedActive.size,
                            nextAppointment  = updatedActive.firstOrNull()
                        )
                    } else current
                }
            }

            try {
                repository.updateAppointmentStatus(
                    appointmentId  = appointmentId,
                    newStatus      = newStatus,
                    rejectionReason = rejectionReason
                )

                // sync fresh data from server no loading show
                fetchAppointmentsForCurrentChef()
                loadDashboardData()

            } catch (e: Exception) {
                Log.e("ChefPortalVM", "Error updating appointment: ${e.message}", e)

                // Rollback both states to pre-optimistic snapshot
                _appointmentsUiState.value = previousAppointmentsState
                _homeUiState.value         = previousHomeState

                _uiEvent.emit("Failed to update appointment: ${e.localizedMessage}")
            }
        }
    }
}

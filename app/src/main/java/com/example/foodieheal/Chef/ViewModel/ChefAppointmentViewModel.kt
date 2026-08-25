package com.example.foodieheal.Chef.ViewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.model.Appointment
import com.example.foodieheal.User.Model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val totalCount: Int,
        val nextAppointment: Appointment?,
        val usersMap: Map<String, User> = emptyMap()
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

class ChefPortalViewModel : ViewModel() {

    private val client = SupabaseClient.client

    // Appointments screen state flow
    private val _appointmentsUiState = MutableStateFlow<AppointmentsUiState>(AppointmentsUiState.Loading)
    val appointmentsUiState: StateFlow<AppointmentsUiState> = _appointmentsUiState.asStateFlow()

    // Home screen state flow
    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    var selectedAppointment by mutableStateOf<Appointment?>(null)
        private set

    init {
        fetchAppointmentsForCurrentChef()
        loadDashboardData()
    }

    // Call this for AppointmentsScreen
    fun fetchAppointmentsForCurrentChef() {
        viewModelScope.launch {
            _appointmentsUiState.value = AppointmentsUiState.Loading
            try {
                val currentUserId = client.auth.currentUserOrNull()?.id

                if (currentUserId.isNullOrEmpty()) {
                    _appointmentsUiState.value = AppointmentsUiState.Error("User not logged in.")
                    return@launch
                }

                val appointments = withContext(Dispatchers.IO) {
                    client.from("Appointment")
                        .select {
                            filter {
                                eq("chefId", currentUserId)
                            }
                        }
                        .decodeList<Appointment>()
                }

                // Batch fetch associated users
                val usersMap = fetchUsersForAppointments(appointments)

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
                val currentUserId = client.auth.currentUserOrNull()?.id

                if (currentUserId.isNullOrEmpty()) {
                    _homeUiState.value = HomeUiState.Error("User not logged in")
                    return@launch
                }

                val appointments = withContext(Dispatchers.IO) {
                    client.from("Appointment")
                        .select {
                            filter {
                                eq("chefId", currentUserId)
                            }
                        }
                        .decodeList<Appointment>()
                }

                val activeAppointments = appointments.filter {
                    it.Status.lowercase() != "cancelled" && it.Status.lowercase() != "completed"
                }

                val nextApp = activeAppointments.firstOrNull()

                // Batch fetch user details (for the next appointment card)
                val usersMap = fetchUsersForAppointments(listOfNotNull(nextApp))

                _homeUiState.value = HomeUiState.Success(
                    totalCount = activeAppointments.size,
                    nextAppointment = nextApp,
                    usersMap = usersMap
                )

            } catch (e: Exception) {
                Log.e("ChefHomeVM", "Error loading home dashboard", e)
                _homeUiState.value = HomeUiState.Error(
                    e.localizedMessage ?: "Failed to load dashboard"
                )
            }
        }
    }

    private suspend fun fetchUsersForAppointments(appointments: List<Appointment>): Map<String, User> {
        val userIds = appointments.map { it.userId }.distinct().filter { it.isNotBlank() }
        if (userIds.isEmpty()) return emptyMap()

        return try {
            withContext(Dispatchers.IO) {
                client.from("users")
                    .select {
                        filter {
                            isIn("id", userIds)
                        }
                    }
                    .decodeList<User>()
                    .associateBy { it.id ?: "" }
            }
        } catch (e: Exception) {
            Log.e("ChefPortalVM", "Error batch-fetching users", e)
            emptyMap()
        }
    }

    fun selectAppointment(appointment: Appointment?) {
        selectedAppointment = appointment
    }

    fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        rejectionReason: String? = null
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Map<String, String>
                    val updateData = buildMap<String, String> {
                        put("Status", newStatus)
                        if (!rejectionReason.isNullOrBlank()) {
                            put("Reject_Reason", rejectionReason)
                        }
                    }

                    client.from("Appointment")
                        .update(updateData) {
                            filter {
                                eq("AppointmentID", appointmentId)
                            }
                        }
                }

                // Refresh UI state
                fetchAppointmentsForCurrentChef()
                loadDashboardData()

            } catch (e: Exception) {
                Log.e("ChefPortalVM", "Error updating appointment: ${e.message}", e)
            }
        }
    }
}
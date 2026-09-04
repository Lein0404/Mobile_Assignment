package com.example.foodieheal.Chef.ViewModel

import android.app.Application
import android.util.Log
import com.example.foodieheal.R
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
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.Chef.notification.ChefNotificationHelper
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.Job
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

    private fun resString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    // Pending appointments count StateFlow (for bottom navigation badge and notification triggers)
    private val _pendingAppointmentsCount = MutableStateFlow(0)
    val pendingAppointmentsCount: StateFlow<Int> = _pendingAppointmentsCount.asStateFlow()

    private var hasNotifiedInitialPending = false

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
                // Clear existing entry to force re-evaluation of recipe status (e.g. if deleted)
                _attachedRecipes.update { it - appointmentId }
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

    // Realtime alert message StateFlow (displayed on ChefHome)
    private val _realtimeAlert = MutableStateFlow<String?>(null)
    val realtimeAlert: StateFlow<String?> = _realtimeAlert.asStateFlow()

    fun dismissRealtimeAlert() {
        _realtimeAlert.value = null
    }

    private var realtimeJob: Job? = null
    private var realtimeChannel: RealtimeChannel? = null

    fun startRealtimeSubscription(chefId: String) {
        if (chefId.isBlank()) return
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                val channel = SupabaseClient.client.channel("chef_appointments_$chefId")
                realtimeChannel = channel
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "Appointment"
                }

                channel.subscribe()
                Log.d("ChefPortalVM", "Subscribed to Supabase Realtime channel for chef: $chefId")

                changeFlow.collect { action ->
                    Log.d("ChefPortalVM", "Realtime action on Appointment table: $action")
                    val record = when (action) {
                        is PostgresAction.Insert -> action.record
                        is PostgresAction.Update -> action.record
                        is PostgresAction.Delete -> action.oldRecord
                        else -> null
                    }
                    val eventChefId = record?.get("chefId")?.let {
                        try { it.toString().replace("\"", "") } catch (_: Exception) { null }
                    } ?: record?.get("ChefID")?.let {
                        try { it.toString().replace("\"", "") } catch (_: Exception) { null }
                    }

                    if (eventChefId != null && eventChefId != chefId) {
                        return@collect
                    }

                    when (action) {
                        is PostgresAction.Insert -> {
                            fetchAppointmentsForCurrentChef()
                            loadDashboardData()
                            _realtimeAlert.value = resString(R.string.chef_realtime_new_booking)
                            ChefNotificationHelper.showPendingAppointmentNotification(
                                context = getApplication(),
                                pendingCount = _pendingAppointmentsCount.value.coerceAtLeast(0) + 1,
                                clientName = null
                            )
                        }
                        is PostgresAction.Update -> {
                            fetchAppointmentsForCurrentChef()
                            loadDashboardData()
                            val newStatus = record?.get("Status")?.let {
                                try { it.toString().replace("\"", "").trim() } catch (_: Exception) { null }
                            }
                            if (newStatus?.equals("Confirmed", ignoreCase = true) == true) {
                                val apptId = record?.get("AppointmentID")?.let {
                                    try { it.toString().replace("\"", "") } catch (_: Exception) { null }
                                }
                                val date = record?.get("Date")?.let {
                                    try { it.toString().replace("\"", "") } catch (_: Exception) { null }
                                }
                                val startTime = record?.get("Start_Time")?.let {
                                    try { it.toString().replace("\"", "") } catch (_: Exception) { null }
                                }
                                val endTime = record?.get("End_Time")?.let {
                                    try { it.toString().replace("\"", "") } catch (_: Exception) { null }
                                }
                                val apptTime = if (!startTime.isNullOrBlank() && !endTime.isNullOrBlank()) "$startTime - $endTime" else startTime
                                val price = record?.get("Total_Price")?.let {
                                    try { it.toString().replace("\"", "").toDoubleOrNull() } catch (_: Exception) { null }
                                }
                                val userId = record?.get("userId")?.let {
                                    try { it.toString().replace("\"", "") } catch (_: Exception) { null }
                                }

                                viewModelScope.launch {
                                    val clientUser = userId?.let { id ->
                                        (_appointmentsUiState.value as? AppointmentsUiState.Success)?.usersMap?.get(id)
                                            ?: (_homeUiState.value as? HomeUiState.Success)?.usersMap?.get(id)
                                            ?: try {
                                                com.example.foodieheal.SupabaseClient.client.from("users")
                                                    .select { filter { eq("id", id) } }
                                                    .decodeSingleOrNull<com.example.foodieheal.User.Model.User>()
                                            } catch (_: Exception) { null }
                                    }

                                    ChefNotificationHelper.showConfirmedAppointmentNotification(
                                        context = getApplication(),
                                        clientName = clientUser?.name,
                                        appointmentDate = date,
                                        appointmentTime = apptTime,
                                        totalAmount = price,
                                        appointmentId = apptId
                                    )
                                }
                            }
                            _realtimeAlert.value = resString(R.string.chef_realtime_booking_updated)
                        }
                        is PostgresAction.Delete -> {
                            fetchAppointmentsForCurrentChef()
                            loadDashboardData()
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("ChefPortalVM", "Error in Supabase Realtime subscription: ${e.localizedMessage}", e)
            }
        }
    }

    init {
        // Initialize notification channel
        ChefNotificationHelper.createNotificationChannel(application)
        observeNetworkStatus()
        fetchAppointmentsForCurrentChef()
        loadDashboardData()

        val currentUserId = repository.getCurrentUserId()
        if (!currentUserId.isNullOrEmpty()) {
            startRealtimeSubscription(currentUserId)
        }
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

    private fun updatePendingCountAndNotify(
        appointments: List<Appointment>,
        usersMap: Map<String, User> = emptyMap()
    ) {
        val pendingList = appointments.filter { it.Status.equals("Pending", ignoreCase = true) }
        val newPendingCount = pendingList.size
        val prevPendingCount = _pendingAppointmentsCount.value

        _pendingAppointmentsCount.value = newPendingCount

        // Fire notification if there are pending appointments and either new arrived or first launch
        if (newPendingCount > 0 && (newPendingCount > prevPendingCount || !hasNotifiedInitialPending)) {
            hasNotifiedInitialPending = true
            val latestAppt = pendingList.firstOrNull()
            val clientUser = latestAppt?.userId?.let { usersMap[it] }
            ChefNotificationHelper.showPendingAppointmentNotification(
                context = getApplication(),
                pendingCount = newPendingCount,
                clientName = clientUser?.name
            )
        } else if (newPendingCount == 0 && prevPendingCount > 0) {
            ChefNotificationHelper.cancelNotification(getApplication())
        }
    }

    // Call this for AppointmentsScreen
    fun fetchAppointmentsForCurrentChef() {
        viewModelScope.launch {
            _appointmentsUiState.value = AppointmentsUiState.Loading
            try {
                val currentUserId = repository.getCurrentUserId()

                if (currentUserId.isNullOrEmpty()) {
                    _appointmentsUiState.value = AppointmentsUiState.Error(resString(R.string.error_chef_user_not_logged_in))
                    return@launch
                }

                val appointments = repository.fetchAppointmentsForChef(currentUserId)

                // Batch fetch associated users
                val usersMap = repository.fetchUsersForAppointments(appointments)

                // Update pending count & dispatch notification if new requests exist
                updatePendingCountAndNotify(appointments, usersMap)

                _appointmentsUiState.value = AppointmentsUiState.Success(
                    appointments = appointments,
                    usersMap = usersMap
                )

            } catch (e: Exception) {
                Log.e("AppointmentsVM", "Error fetching appointments", e)
                _appointmentsUiState.value = AppointmentsUiState.Error(
                    e.localizedMessage ?: resString(R.string.error_chef_load_appointments_failed)
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
                    _homeUiState.value = HomeUiState.Error(resString(R.string.error_chef_user_not_logged_in))
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

                // Update pending count & badge
                updatePendingCountAndNotify(appointments, usersMap)

                _homeUiState.value = HomeUiState.Success(
                    totalCount = activeAppointments.size,
                    nextAppointment = nextApp,
                    usersMap = usersMap,
                    allAppointments = appointments
                )

            } catch (e: Exception) {
                Log.e("ChefHomeVM", "Error loading home dashboard", e)
                _homeUiState.value = HomeUiState.Error(
                    e.localizedMessage ?: resString(R.string.error_chef_load_dashboard_failed)
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
                _uiEvent.emit(resString(R.string.error_chef_update_offline))
                return@launch
            }

            // Snapshot current state for rollback
            val previousAppointmentsState = _appointmentsUiState.value
            val previousHomeState = _homeUiState.value
            val previousPendingCount = _pendingAppointmentsCount.value

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
                val updatedAppts = applyOptimisticUpdate(previousAppointmentsState.appointments)
                val updatedPending = updatedAppts.count { it.Status.equals("Pending", ignoreCase = true) }
                _pendingAppointmentsCount.value = updatedPending
                if (updatedPending == 0) {
                    ChefNotificationHelper.cancelNotification(getApplication())
                }

                _appointmentsUiState.update { current ->
                    if (current is AppointmentsUiState.Success) {
                        current.copy(appointments = updatedAppts)
                    } else current
                }
            }

            // Update Home dashboard state
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
                _pendingAppointmentsCount.value = previousPendingCount

                _uiEvent.emit(resString(R.string.error_chef_update_appointment_failed, e.localizedMessage ?: ""))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
        viewModelScope.launch {
            try {
                realtimeChannel?.unsubscribe()
            } catch (_: Exception) {}
        }
    }
}

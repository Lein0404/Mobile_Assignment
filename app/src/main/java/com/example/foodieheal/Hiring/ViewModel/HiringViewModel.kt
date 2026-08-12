package com.example.foodieheal.Hiring.ViewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient.client
import com.example.foodieheal.model.Appointment
import com.example.foodieheal.model.User
import com.example.mobileassignmentloginpart.Model.Chef
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

sealed interface AppointmentValidationError {
    object InvalidTime : AppointmentValidationError
    object TimeSlotOccupied : AppointmentValidationError
    object InvalidAddress : AppointmentValidationError
    object InvalidPostcode : AppointmentValidationError
    object InvalidState : AppointmentValidationError
    object InvalidServingSize : AppointmentValidationError
    object InvalidDescription : AppointmentValidationError
}

sealed interface UserAppointmentsUiState {
    object Loading : UserAppointmentsUiState
    data class Success(
        val appointments: List<Appointment>,
        val usersMap: Map<String, User> = emptyMap()
    ) : UserAppointmentsUiState
    data class Error(val message: String) : UserAppointmentsUiState
}

data class ChefAppointmentsUiState(
    val isLoading: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val chefUser: User? = null,
    val errorMessage: String? = null
)

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
    val hasInvalidTimeError: Boolean
        get() = errors.contains(AppointmentValidationError.InvalidTime)

    val hasTimeSlotOccupiedError: Boolean
        get() = errors.contains(AppointmentValidationError.TimeSlotOccupied)

    val canSubmit: Boolean
        get() = isTimeValid &&
                isAddressValid &&
                isPostcodeValid &&
                isStateValid &&
                isServingSizeValid &&
                isDescriptionValid

}

class HiringViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentUiState())
    private val _userAppointmentsState = MutableStateFlow<UserAppointmentsUiState>(UserAppointmentsUiState.Loading)
    val userAppointmentsState: StateFlow<UserAppointmentsUiState> = _userAppointmentsState.asStateFlow()

    private val _chefAppointmentsState = MutableStateFlow(ChefAppointmentsUiState())
    val chefAppointmentsState: StateFlow<ChefAppointmentsUiState> = _chefAppointmentsState.asStateFlow()
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()

    private val _averageRating = MutableStateFlow(0.0)
    val averageRating: StateFlow<Double> = _averageRating.asStateFlow()

    private val _totalReviews = MutableStateFlow(0)
    val totalReviews: StateFlow<Int> = _totalReviews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    var chefList by mutableStateOf<List<Chef>>(emptyList())
        private set

    var appointmentList by mutableStateOf<List<Appointment>>(emptyList())
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedChef by mutableStateOf<Chef?>(null)
        private set

    var selectedDate by mutableStateOf<LocalDate>(LocalDate.now())
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var selectedTabIndex by mutableIntStateOf(0)
        private set

    fun onTabSelected(index: Int) {
        selectedTabIndex = index
    }

    init {
        fetchAppointmentsForCurrentUser()
    }

    fun selectChef(chef: Chef) {
        selectedChef = chef
    }

    fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
    }

    fun clearSelectedChef() {
        selectedChef = null
    }

    private fun validateTimeSlot(timeSlot: String) {
        val isOccupied = checkTimeSlotOverlap(timeSlot)
        val currentErrors = _uiState.value.errors.toMutableSet()

        if (timeSlot.isBlank()) {
            currentErrors.add(AppointmentValidationError.InvalidTime)
            currentErrors.remove(AppointmentValidationError.TimeSlotOccupied)
        } else if (isOccupied) {
            currentErrors.remove(AppointmentValidationError.InvalidTime)
            currentErrors.add(AppointmentValidationError.TimeSlotOccupied)
        } else {
            currentErrors.remove(AppointmentValidationError.InvalidTime)
            currentErrors.remove(AppointmentValidationError.TimeSlotOccupied)
        }

        _uiState.update {
            it.copy(
                isTimeSlotOccupied = isOccupied,
                errors = currentErrors
            )
        }
    }

    private fun checkTimeSlotOverlap(
        timeSlot: String,
        targetDateStr: String? = null,
        currentAppointmentId: String? = null
    ): Boolean {
        if (!timeSlot.contains(" - ")) return false
        val parts = timeSlot.split(" - ")
        if (parts.size != 2) return false

        val newStart = parseTimeToMinutes(parts[0])
        val newEnd = parseTimeToMinutes(parts[1])

        if (newStart == null || newEnd == null || newEnd <= newStart) {
            Log.e("OverlapCheck", "Invalid input time slot format: $timeSlot (start=$newStart, end=$newEnd)")
            return false
        }

        val existingAppointments: List<Appointment> = when (val state = _chefAppointmentsState.value) {
            is List<*> -> state.filterIsInstance<Appointment>()
            is ChefAppointmentsUiState -> state.appointments
            else -> emptyList()
        }
        Log.d("OverlapCheck", "Checking against ${existingAppointments.size} loaded appointments for current chef.")

        // Determine target date string for comparison
        val targetDate = targetDateStr?.trim() ?: selectedDate.toString()

        return existingAppointments.any { appt ->
            // Ignore the appointment that be reschedule so it does not confict with it own
            if (currentAppointmentId != null && appt.AppointmentID == currentAppointmentId) {
                return@any false
            }

            // Ignore cancelled or rejected bookings
            val status = appt.Status?.lowercase(java.util.Locale.US) ?: ""
            val isActive = status !in listOf("cancelled", "rejected")

            // Date Matching (Handles both direct String comparison and LocalDate formats)
            val apptDate = appt.Date?.trim().orEmpty()
            val isSameDate = apptDate.equals(targetDate, ignoreCase = true) ||
                    apptDate.contains(targetDate) ||
                    targetDate.contains(apptDate)

            // Extract Start & End Time
            val rawStart = appt.Start_Time
            val rawEnd = appt.End_Time

            val existingStart = parseTimeToMinutes(rawStart)
            val existingEnd = parseTimeToMinutes(rawEnd)

            Log.d("OverlapCheck", "Appt ID: ${appt.AppointmentID} | Date: $apptDate (Matches: $isSameDate) | " +
                    "Status: $status (Active: $isActive) | RawStart: $rawStart ($existingStart mins) | " +
                    "RawEnd: $rawEnd ($existingEnd mins)")

            if (isActive && isSameDate && existingStart != null && existingEnd != null) {
                // Overlap formula: (RequestedStart < BookedEnd) AND (RequestedEnd > BookedStart)
                val isOverlapping = (newStart < existingEnd) && (newEnd > existingStart)
                if (isOverlapping) {
                    Log.w("OverlapCheck", "⚠️ OVERLAP DETECTED with Appointment ID: ${appt.AppointmentID}")
                    return@any true
                }
            }
            false
        }
    }

    private fun parseTimeToMinutes(timeStr: String?): Int? {
        if (timeStr.isNullOrBlank()) return null
        val cleanStr = timeStr.trim().uppercase(java.util.Locale.US)

        // Formats to try sequentially
        val formats = listOf(
            "hh:mm a",
            "h:mm a",
            "HH:mm:ss",
            "HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                val date = sdf.parse(cleanStr)
                if (date != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = date }
                    return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                }
            } catch (_: Exception) {

            }
        }
        return null
    }

    // Value update handlers
    // Value update handlers
    fun onAppointmentTimeChanged(time: String) {
        _uiState.update { it.copy(appointmentTime = time) }
        validateTimeSlot(time)
        revalidateIfSubmitted()
    }

    fun onAddressChanged(address: String) {
        _uiState.update { it.copy(address = address) }
        revalidateIfSubmitted()
    }

    fun onPostcodeChanged(postcode: String) {
        _uiState.update { it.copy(postcode = postcode) }
        revalidateIfSubmitted()
    }

    fun onStateChanged(state: String) {
        _uiState.update { it.copy(state = state) }
        revalidateIfSubmitted()
    }

    fun onServingSizeChanged(servingSize: String) {
        _uiState.update { it.copy(servingSize = servingSize) }
        revalidateIfSubmitted()
    }

    fun onHealthPreferenceChanged(healthPreference: String) {
        _uiState.update { it.copy(healthPreference = healthPreference) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
        revalidateIfSubmitted()
    }

    // Resets form state
    fun clearData() {
        _uiState.value = AppointmentUiState()
        errorMessage = null
    }

    // Single source of validation logic
    fun validateFormValues(
        appointmentTime: String,
        address: String,
        postcode: String,
        state: String,
        servingSize: String,
        description: String,
        targetDate: String? = null,
        currentAppointmentId: String? = null,
        isTimeValid: Boolean = appointmentTime.isNotBlank(),
        isAddressValid: Boolean = address.isNotBlank(),
        isPostcodeValid: Boolean = postcode.matches(Regex("^[0-9]{5}$")),
        isStateValid: Boolean = state.isNotBlank(),
        isServingSizeValid: Boolean = (servingSize.toIntOrNull() ?: 0) > 0,
        isDescriptionValid: Boolean = description.isNotBlank()
    ): Set<AppointmentValidationError> {
        val newErrors = mutableSetOf<AppointmentValidationError>()

        // Time Validation
        if (appointmentTime.isBlank()) {
            newErrors.add(AppointmentValidationError.InvalidTime)
        } else if (checkTimeSlotOverlap(appointmentTime, targetDate, currentAppointmentId)) {
            newErrors.add(AppointmentValidationError.TimeSlotOccupied)
        }

        if (!isTimeValid) newErrors.add(AppointmentValidationError.InvalidTime)
        if (!isAddressValid) newErrors.add(AppointmentValidationError.InvalidAddress)
        if (!isPostcodeValid) newErrors.add(AppointmentValidationError.InvalidPostcode)
        if (!isStateValid) newErrors.add(AppointmentValidationError.InvalidState)
        if (!isServingSizeValid) newErrors.add(AppointmentValidationError.InvalidServingSize)
        if (!isDescriptionValid) newErrors.add(AppointmentValidationError.InvalidDescription)

        return newErrors
    }

    fun validateAndSubmit(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        val newErrors = validateFormValues(
            appointmentTime = currentState.appointmentTime,
            address = currentState.address,
            postcode = currentState.postcode,
            state = currentState.state,
            servingSize = currentState.servingSize,
            description = currentState.description,
            isTimeValid = currentState.isTimeValid,
            isAddressValid = currentState.isAddressValid,
            isPostcodeValid = currentState.isPostcodeValid,
            isStateValid = currentState.isStateValid,
            isServingSizeValid = currentState.isServingSizeValid,
            isDescriptionValid = currentState.isDescriptionValid
        )

        _uiState.update {
            it.copy(
                hasAttemptedSubmit = true,
                errors = newErrors,
                isTimeSlotOccupied = newErrors.contains(AppointmentValidationError.TimeSlotOccupied)
            )
        }

        if (newErrors.isEmpty()) {
            onSuccess()
        }
    }

    private fun revalidateIfSubmitted() {
        if (_uiState.value.hasAttemptedSubmit) {
            validateAndSubmit(onSuccess = {})
        }
    }

    fun validateAndReschedule(
        appointmentId: String,
        newDate: String,
        newStartTime: String,
        newEndTime: String,
        newAddress: String,
        newPostcode: String,
        newState: String,
        newServingSize: String,
        newDescription: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ): Set<AppointmentValidationError> {
        val timeRangeString = "$newStartTime - $newEndTime"

        // Pass targetDate and currentAppointmentId to check overlaps against the new date
        // and ignore self-conflicts
        val errors = validateFormValues(
            appointmentTime = timeRangeString,
            address = newAddress,
            postcode = newPostcode,
            state = newState,
            servingSize = newServingSize,
            description = newDescription,
            targetDate = newDate,
            currentAppointmentId = appointmentId
        )

        if (errors.isEmpty()) {
            rescheduleAppointment(
                appointmentId = appointmentId,
                newDate = newDate,
                newStartTime = newStartTime,
                newEndTime = newEndTime,
                newAddress = newAddress,
                newPostcode = newPostcode,
                newState = newState,
                newServingSize = newServingSize.toIntOrNull() ?: 1,
                newDescription = newDescription,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        return errors
    }

    fun fetchAllChefs() {
        viewModelScope.launch {
            isProcessing = true
            errorMessage = null

            try {
                // Fetch approved chefs from Supabase
                val chefs = client.postgrest["Chef"]
                    .select {
                        filter {
                            ilike("Status", "approved")
                        }
                    }
                    .decodeList<Chef>()

                val ratedAppointments = client.postgrest["Appointment"]
                    .select {
                        filter {
                            gt("rating", 0)
                        }
                    }
                    .decodeList<Appointment>()

                val processedChefs = chefs
                    .filter { chef ->
                        // Ensure Pricing is not null
                        chef.Pricing != null && chef.Pricing >= 0
                    }
                    .map { chef ->
                        val idToMatch = chef.chefId.ifEmpty { chef.id }

                        // Calculate average rating for this specific chef
                        val chefRatings = ratedAppointments
                            .filter { it.chefId == idToMatch }
                            .mapNotNull { it.rating }

                        val avgRating = if (chefRatings.isNotEmpty()) {
                            (chefRatings.average() * 10).toInt() / 10.0
                        } else null

                        // Return chef copy with calculated rating
                        chef.copy(averagerating = avgRating)
                    }

                Log.d("SupabaseChef", "Successfully loaded ${chefs.size} chefs.")
                chefList = processedChefs

            } catch (e: CancellationException) {
                // Re-throw so Kotlin can cleanly handle coroutine cancellation
                throw e
            } catch (e: Exception) {
                Log.e("SupabaseChef", "Error decoding chefs list", e)
                errorMessage = e.message ?: "Failed to fetch chef profiles"
            } finally {
                isProcessing = false
            }
        }
    }

    fun fetchAppointmentsForChef(chefId: String) {
        if (chefId.isBlank()) return
        viewModelScope.launch {
            try {
                val appointments = withContext(Dispatchers.IO) {
                    client.from("Appointment")
                        .select {
                            filter {
                                eq("chefId", chefId)
                            }
                        }
                        .decodeList<Appointment>()
                }

                val chefUser = withContext(Dispatchers.IO) {
                    client.from("users") //
                        .select {
                            filter {
                                eq("id", chefId)
                            }
                        }
                        .decodeSingleOrNull<User>()
                }

                _chefAppointmentsState.value = ChefAppointmentsUiState(
                    isLoading = false,
                    appointments = appointments,
                    chefUser = chefUser
                )
                Log.d(
                    "HiringViewModel",
                    "Loaded ${appointments.size} appointments for chef: $chefId"
                )
            } catch (e: Exception) {
                Log.e("HiringViewModel", "Error fetching appointments for chef $chefId", e)
                _chefAppointmentsState.value = ChefAppointmentsUiState(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load details"
                )
            }
        }
    }

    fun calculateTotalPrice(): Double {
        val hourlyRate = selectedChef?.Pricing ?: 0.0
        val timeString = uiState.value.appointmentTime

        if (!timeString.contains(" - ")) return hourlyRate

        val parts = timeString.split(" - ")
        if (parts.size != 2) return hourlyRate

        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        return try {
            val startDate = sdf.parse(parts[0].trim())
            val endDate = sdf.parse(parts[1].trim())

            if (startDate != null && endDate != null) {
                val diffInMillis = endDate.time - startDate.time
                val hours = diffInMillis.toDouble() / (1000 * 60 * 60)

                // Ensure at least 1 hour minimum multiplier
                val actualHours = if (hours > 0) hours else 1.0
                hourlyRate * actualHours
            } else {
                hourlyRate
            }
        } catch (e: Exception) {
            hourlyRate
        }
    }

    fun fetchAppointmentsForCurrentUser() {
        viewModelScope.launch {
            _userAppointmentsState.value = UserAppointmentsUiState.Loading
            try {
                val currentUserId = client.auth.currentUserOrNull()?.id

                if (currentUserId.isNullOrEmpty()) {
                    _userAppointmentsState.value = UserAppointmentsUiState.Error("User not logged in.")
                    return@launch
                }

                // Fetch appointments where current user is the client
                val appointments = withContext(Dispatchers.IO) {
                    client.from("Appointment")
                        .select {
                            filter {
                                eq("userId", currentUserId)
                            }
                        }
                        .decodeList<Appointment>()
                }

                // Batch fetch associated Chef profiles mapped by appointment.chefId
                val chefsMap = fetchChefsForAppointments(appointments)

                _userAppointmentsState.value = UserAppointmentsUiState.Success(
                    appointments = appointments,
                    usersMap = chefsMap
                )

            } catch (e: Exception) {
                Log.e("HiringViewModel", "Error fetching user appointments", e)
                _userAppointmentsState.value = UserAppointmentsUiState.Error(
                    e.localizedMessage ?: "Failed to load appointments"
                )
            }
        }
    }

    // Maps appointment.chefId directly to User object
    private suspend fun fetchChefsForAppointments(appointments: List<Appointment>): Map<String, User> {
        // 1. Grab all unique chefIds from appointments
        val chefIds = appointments.mapNotNull { it.chefId }.distinct().filter { it.isNotBlank() }
        if (chefIds.isEmpty()) return emptyMap()

        return try {
            withContext(Dispatchers.IO) {

                val chefRecords = client.from("Chef")
                    .select {
                        filter { isIn("chefId", chefIds) }
                    }
                    .decodeList<Chef>()

                // Map chefId -> User UI object directly from Chef table fields
                chefRecords.associate { chef ->
                    val key = chef.chefId.ifEmpty { chef.id }

                    key to User(
                        id = key,
                        name = chef.name,
                        profilePicUrl = chef.profilePictureUrl ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("HiringViewModel", "Error fetching chefs from Chef table", e)
            emptyMap()
        }
    }

    fun createAppointment(
        userId: String,
        chefId: String,
        selectedDate: String,
        startTime: String,
        endTime: String,
        totalPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId.isBlank() || chefId.isBlank()) {
            onError("Invalid user or chef ID.")
            return
        }

        isSubmitting = true
        errorMessage = null

        val state = uiState.value

        // Put here first dont know need or not in future
        val randomNum = (100..999).random()
        val customAppointmentId = "A$randomNum"
        val parsedServingSize = state.servingSize.toIntOrNull() ?: 1

        val newAppointment = Appointment(
            Date = selectedDate,
            Start_Time = startTime,
            End_Time = endTime,
            Address = state.address.trim(),
            Postcode = state.postcode.trim(),
            State = state.state,
            Note = state.description.trim(),
            Serving_Size = parsedServingSize,
            Health_Preference = state.healthPreference,
            Total_Price = totalPrice,
            Status = "Pending",
            rating = null,
            Comment = null,
            Reject_Reason = null,
            chefId = chefId,
            userId = userId
        )

        viewModelScope.launch {
            try {
                // Insert record into Supabase
                client.from("Appointment").insert(newAppointment)

                isSubmitting = false
                clearData()
                onSuccess()

            } catch (e: Exception) {
                isSubmitting = false
                Log.e("Appointment", "Error creating appointment", e)

                val msg = e.message ?: "Failed to book appointment. Please try again."
                errorMessage = when {
                    msg.contains("row-level security", ignoreCase = true) ||
                            msg.contains("violates row-level security policy", ignoreCase = true) ->
                        "Supabase RLS Error: Check INSERT policy on 'Appointment' table."
                    msg.contains("duplicate key", ignoreCase = true) ->
                        "Appointment ID conflict. Please try again."
                    else -> msg.lines().firstOrNull() ?: msg
                }
                onError(e.message ?: "An error occurred")
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
                val ErrorMessage = "Failed to cancel appointment: $rawError"
                onError(ErrorMessage)
            }
        )
    }

    fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val updateData = buildJsonObject {
                        put("Status", newStatus)
                    }

                    client.from("Appointment")
                        .update(updateData) {
                            filter {
                                eq("AppointmentID", appointmentId)
                            }
                        }
                }

                // Refresh user appointments list
                fetchAppointmentsForCurrentUser()

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("HiringViewModel", "Error updating status", e)
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Failed to update appointment status")
                }
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
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Build a JsonObject instead of Map<String, Any>
                    val updateData = buildJsonObject {
                        put("Date", newDate)
                        put("Start_Time", newStartTime)
                        put("End_Time", newEndTime)
                        put("Address", newAddress)
                        put("Postcode", newPostcode)
                        put("State", newState)
                        put("Serving_Size", newServingSize)
                        put("Note", newDescription)
                        put("Status", "Pending") // Require re-approval from Chef after reschedule
                    }

                    client.from("Appointment")
                        .update(updateData) {
                            filter {
                                eq("AppointmentID", appointmentId)
                            }
                        }
                }

                // Refresh appointments list
                fetchAppointmentsForCurrentUser()

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("HiringViewModel", "Error rescheduling appointment", e)
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Failed to reschedule appointment")
                }
            }
        }
    }

    fun submitReview(
        appointmentId: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Update Rating, Comment, and set Status to Completed
                val updateData = buildJsonObject {
                    put("rating", rating)
                    put("Comment", comment)
                }

                client.from("Appointment")
                    .update(updateData) {
                        filter {
                            eq("AppointmentID", appointmentId) // Use "AppointmentID" if that's your primary key column name
                        }
                    }

                // Refresh the user's appointment list so the UI reflects updated rating & comment
                fetchAppointmentsForCurrentUser()

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("HiringViewModel", "Error submitting review: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Failed to submit review. Please try again.")
                }
            }
        }
    }

    fun clearAppointmentForm() {
        _uiState.update { currentState ->
            currentState.copy(
                appointmentTime = "",
                address = "",
                postcode = "",
                state = "",
                servingSize = "",
                healthPreference = "",
                description = "",
                hasAttemptedSubmit = false,
                errors = emptySet() // 🟢 Fixed: set to emptySet() instead of emptyList()
            )
        }
    }

    val currentChefId: String
        get() = selectedChef?.let { it.chefId.ifEmpty { it.id } }.orEmpty()
}
package com.example.foodieheal.Hiring.ViewModel

import android.R.attr.description
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Chef.ServingSize
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.SupabaseClient.client
import com.example.foodieheal.model.Appointment
import com.example.mobileassignmentloginpart.Model.Chef
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AppointmentValidationError {
    object InvalidTime : AppointmentValidationError
    object InvalidAddress : AppointmentValidationError
    object InvalidPostcode : AppointmentValidationError
    object InvalidState : AppointmentValidationError
    object InvalidServingSize : AppointmentValidationError
    object InvalidDescription : AppointmentValidationError
}

// Immutable UI State Container
data class AppointmentUiState(
    val appointmentTime: String = "",
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
    // Dynamic validation checks
    val isTimeValid: Boolean get() = appointmentTime.isNotBlank()
    val isAddressValid: Boolean get() = address.isNotBlank()
    val isPostcodeValid: Boolean get() = postcode.matches(Regex("^[0-9]{5}$"))
    val isStateValid: Boolean get() = state.isNotBlank()
    val isServingSizeValid: Boolean get() = servingSize.toIntOrNull()?.let { it > 0 } == true
    val isDescriptionValid: Boolean get() = description.trim().isNotBlank()

    val canSubmit: Boolean
        get() = isTimeValid && isAddressValid && isPostcodeValid && isStateValid && isServingSizeValid && isDescriptionValid
}

class HiringViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()

    var chefList by mutableStateOf<List<Chef>>(emptyList())
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedChef by mutableStateOf<Chef?>(null)
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    fun selectChef(chef: Chef) {
        selectedChef = chef
    }

    fun clearSelectedChef() {
        selectedChef = null
    }

    // Value update handlers
    // Value update handlers
    fun onAppointmentTimeChanged(time: String) {
        _uiState.update { it.copy(appointmentTime = time) }
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
    fun validateAndSubmit(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val newErrors = mutableSetOf<AppointmentValidationError>()

        if (!currentState.isTimeValid) newErrors.add(AppointmentValidationError.InvalidTime)
        if (!currentState.isAddressValid) newErrors.add(AppointmentValidationError.InvalidAddress)
        if (!currentState.isPostcodeValid) newErrors.add(AppointmentValidationError.InvalidPostcode)
        if (!currentState.isStateValid) newErrors.add(AppointmentValidationError.InvalidState)
        if (!currentState.isServingSizeValid) newErrors.add(AppointmentValidationError.InvalidServingSize)
        if (!currentState.isDescriptionValid) newErrors.add(AppointmentValidationError.InvalidDescription)

        _uiState.update {
            it.copy(
                hasAttemptedSubmit = true,
                errors = newErrors
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

                Log.d("SupabaseChef", "Successfully loaded ${chefs.size} chefs.")
                chefList = chefs
            } catch (e: Exception) {
                Log.e("SupabaseChef", "Error decoding chefs list", e)
                errorMessage = e.message ?: "Failed to fetch chef profiles"
            } finally {
                isProcessing = false
            }
        }
    }

    fun createAppointment(
        userId: String,
        chefId: String,
        selectedDate: String,
        startTime: String,
        endTime: String,
        totalPrice: Double,
        onSuccess: () -> Unit
    ) {
        if (userId.isBlank() || chefId.isBlank()) return

        isSubmitting = true
        errorMessage = null

        val state = uiState.value

        // 1. Generate Custom ID & parse serving size integer
        val randomNum = (100..999).random()
        val customAppointmentId = "A$randomNum"
        val parsedServingSize = state.servingSize.toIntOrNull() ?: 1

        // 2. Build payload using state properties
        val newAppointment = Appointment(
            AppointmentID = customAppointmentId,
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
            rating = 0.0,
            chefId = chefId,
            userId = userId
        )

        viewModelScope.launch {
            try {
                // 3. Insert record into Supabase
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
            }
        }
    }
}
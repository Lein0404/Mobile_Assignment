package com.example.foodieheal.Payment.ViewModel

import android.util.Log
import com.example.foodieheal.R
import com.example.foodieheal.MainActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.Payment.data.payment
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.hiring.model.AppointmentPricingBreakdown
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.SupabaseClient as AppSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class PaymentViewModel(
    private val client: SupabaseClient = AppSupabaseClient.client,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private fun resString(resId: Int, vararg args: Any): String? {
        return MainActivity.appContext?.getString(resId, *args)
    }

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            networkMonitor.isConnected.collectLatest { connected ->
                _isNetworkAvailable.value = connected
            }
        }
    }

    fun loadAppointmentById(appointmentId: String) {
        if (appointmentId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val fetchedAppointment = client.from("Appointment")
                    .select {
                        filter {
                            eq("AppointmentID", appointmentId)
                        }
                    }
                    .decodeSingle<Appointment>()

                var breakdown: AppointmentPricingBreakdown? = null
                try {
                    val hiringRepo = HiringRepository()
                    val targetChefId = fetchedAppointment.chefId
                    val chef: Chef? = if (!targetChefId.isNullOrBlank()) {
                        hiringRepo.fetchChefById(targetChefId)
                    } else {
                        null
                    }
                    val attachedRecipes = hiringRepo.fetchAppointmentRecipes(appointmentId)

                    val selectedRecipes = attachedRecipes.mapNotNull { item ->
                        item.recipe?.let { recipe ->
                            com.example.foodieheal.hiring.model.SelectedAppointmentRecipe(
                                recipe = recipe,
                                serviceCount = item.service_count.toInt().coerceAtLeast(1),
                                customNote = item.custom_note.orEmpty(),
                                chefProvidesIngredients = item.chef_provide_ingredient
                            )
                        }
                    }

                    breakdown = com.example.foodieheal.hiring.model.AppointmentPricingBreakdown.calculate(
                        chefHourlyRate = chef?.Pricing ?: 0.0,
                        appointmentTime = "${fetchedAppointment.Start_Time} - ${fetchedAppointment.End_Time}",
                        selectedRecipes = selectedRecipes,
                        userState = fetchedAppointment.State,
                        chefState = chef?.state.orEmpty()
                    )
                } catch (calcEx: Exception) {
                    Log.e("PaymentViewModel", "Error calculating pricing breakdown: ${calcEx.localizedMessage}")
                }

                _uiState.update {
                    it.copy(
                        appointment = fetchedAppointment,
                        pricingBreakdown = breakdown,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: (resString(R.string.error_payment_load_appointment_failed) ?: "Failed to load appointment details.")
                    )
                }
            }
        }
    }

    fun processPayment(
        selectedMethod: PaymentMethod?,
        onSuccess: (transactionId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!_isNetworkAvailable.value) {
            val errorMsg = resString(R.string.error_payment_no_internet) ?: "No internet connection. Please connect to the internet to complete your payment."
            _uiState.update { it.copy(errorMessage = errorMsg) }
            onError(errorMsg)
            return
        }

        val currentAppointment = uiState.value.appointment

        if (currentAppointment == null) {
            val err = resString(R.string.error_payment_appointment_missing) ?: "Appointment information is missing."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        val appointmentId = currentAppointment.AppointmentID
        if (appointmentId.isNullOrEmpty()) {
            val err = resString(R.string.error_payment_invalid_appointment_id) ?: "Invalid Appointment ID."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        if (selectedMethod == null) {
            val err = resString(R.string.error_payment_select_method) ?: "Please select a payment method."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            var isPaymentInserted = false
            var createdPaymentId: String? = null

            try {
                if (selectedMethod is PaymentMethod.InAppWallet) {
                    val walletRepo = com.example.foodieheal.wallet.data.WalletRepository(client)
                    val result = walletRepo.payAppointmentViaWallet(
                        appointmentId = appointmentId,
                        userId = currentAppointment.userId.orEmpty()
                    )

                    result.onSuccess { txnId ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPaymentSuccess = true,
                                paymentTransactionId = txnId
                            )
                        }
                        onSuccess(txnId)
                    }.onFailure { ex ->
                        throw ex
                    }
                    return@launch
                }

                val transactionId = "TXN-${System.currentTimeMillis()}"
                val existingPaymentId = currentAppointment.PaymentId
                val targetPaymentId = if (!existingPaymentId.isNullOrBlank()) existingPaymentId else UUID.randomUUID().toString()
                createdPaymentId = targetPaymentId

                val totalAmount = currentAppointment.Total_Price ?: 0.0

                val methodString = when (selectedMethod) {
                    is PaymentMethod.CreditCard -> "${selectedMethod.cardBrand} (•••• ${selectedMethod.last4Digits})"
                    is PaymentMethod.InAppWallet -> "In-App Wallet"
                }

                val paymentMethodId = when (selectedMethod) {
                    is PaymentMethod.CreditCard -> selectedMethod.id
                    is PaymentMethod.InAppWallet -> null
                }

                val paymentRecord = payment(
                    paymentId = targetPaymentId,
                    transactionId = transactionId,
                    appointmentId = appointmentId,
                    userId = currentAppointment.userId.orEmpty(),
                    totalAmount = totalAmount,
                    paymentMethod = methodString,
                    paymentMethodId = paymentMethodId,
                    status = "Completed",
                    payAt = Instant.now().toString(),
                    createdAt = Instant.now().toString()
                )

                client.from("Payment").upsert(paymentRecord)
                isPaymentInserted = true

                client.from("Appointment").update(
                    {
                        set("Status", "Confirmed")
                        set("PaymentId", targetPaymentId)
                    }
                ) {
                    filter {
                        eq("AppointmentID", appointmentId)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPaymentSuccess = true,
                        paymentTransactionId = transactionId
                    )
                }

                onSuccess(transactionId)

            } catch (e: Exception) {
                // Rollback payment record if updating appointment fails
                if (isPaymentInserted && createdPaymentId != null) {
                    try {
                        client.from("Payment").delete {
                            filter {
                                eq("PaymentID", createdPaymentId)
                            }
                        }
                    } catch (rollbackEx: Exception) {
                        Log.e("PaymentViewModel", "Rollback failed: ${rollbackEx.localizedMessage}")
                    }
                }

                val errorMsg = e.localizedMessage ?: (resString(R.string.error_payment_processing_failed) ?: "Payment processing failed. Please try again.")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
                onError(errorMsg)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val client: SupabaseClient = AppSupabaseClient.client,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
                return PaymentViewModel(client, networkMonitor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
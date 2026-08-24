package com.example.foodieheal.Payment.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Payment.data.payment
import com.example.foodieheal.model.Appointment
import com.example.foodieheal.SupabaseClient as AppSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class PaymentViewModel(
    private val client: SupabaseClient = AppSupabaseClient.client
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

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

                _uiState.update {
                    it.copy(
                        appointment = fetchedAppointment,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load appointment details."
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
        val currentAppointment = uiState.value.appointment

        if (currentAppointment == null) {
            val err = "Appointment information is missing."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        val appointmentId = currentAppointment.AppointmentID
        if (appointmentId.isNullOrEmpty()) {
            val err = "Invalid Appointment ID."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        if (selectedMethod == null) {
            val err = "Please select a payment method."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            var isPaymentInserted = false
            var createdPaymentId: String? = null

            try {
                val transactionId = "TXN-${System.currentTimeMillis()}"
                val generatedPaymentId = UUID.randomUUID().toString()
                createdPaymentId = generatedPaymentId

                val totalAmount = currentAppointment.Total_Price ?: 0.0

                val methodString = when (selectedMethod) {
                    is PaymentMethod.CreditCard -> "${selectedMethod.cardBrand} (•••• ${selectedMethod.last4Digits})"
                }

                val paymentMethodId = selectedMethod.id

                val paymentRecord = payment(
                    paymentId = generatedPaymentId,
                    transactionId = transactionId,
                    appointmentId = appointmentId,
                    userId = currentAppointment.userId.orEmpty(),
                    totalAmount = totalAmount,
                    paymentMethod = methodString,
                    paymentMethodId = paymentMethodId,
                    status = "Completed"
                )

                client.from("Payment").insert(paymentRecord)
                isPaymentInserted = true

                client.from("Appointment").update(
                    {
                        set("Status", "Confirmed")
                        set("PaymentId", generatedPaymentId)
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
                                // Matching column casing to PaymentID
                                eq("PaymentID", createdPaymentId)
                            }
                        }
                    } catch (rollbackEx: Exception) {
                        Log.e("PaymentViewModel", "Rollback failed: ${rollbackEx.localizedMessage}")
                    }
                }

                val errorMsg = e.localizedMessage ?: "Payment processing failed. Please try again."
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
}
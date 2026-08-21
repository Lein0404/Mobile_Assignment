package com.example.foodieheal.Payment.ViewModel

import com.example.foodieheal.model.Appointment

sealed class PaymentMethod(
    open val id: String,
    open val title: String,
    open val subtitle: String? = null
) {
    data class CreditCard(
        override val id: String,
        val last4Digits: String,
        val cardBrand: String,
        val expiryDate: String? = null,
        val isDefault: Boolean = false
    ) : PaymentMethod(
        id = id,
        title = "$cardBrand •••• $last4Digits",
        subtitle = expiryDate?.let { "Expires $it" } ?: "Saved Card"
    )
}

data class PaymentUiState(
    val isLoading: Boolean = false,
    val appointment: Appointment? = null,
    val availableMethods: List<PaymentMethod> = emptyList(),
    val selectedMethod: PaymentMethod? = null,
    val isPaymentSuccess: Boolean = false,
    val paymentTransactionId: String? = null,
    val errorMessage: String? = null
)

data class PaymentMethodUiState(
    val isLoading: Boolean = false,
    val availableMethods: List<PaymentMethod> = emptyList(),
    val selectedMethod: PaymentMethod? = null,
    val errorMessage: String? = null
)


data class NewCardFormState(
    val cardNumber: String = "",
    val cardHolderName: String = "",
    val expiryDate: String = "",
    val cvv: String = "",
    val isSaveForFuture: Boolean = true
)
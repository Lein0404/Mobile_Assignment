package com.example.foodieheal.wallet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WalletTransactionType {
    @SerialName("TOP_UP")
    TOP_UP,

    @SerialName("APPOINTMENT_PAYMENT")
    APPOINTMENT_PAYMENT,

    @SerialName("REFUND")
    REFUND,

    @SerialName("RESCHEDULE_ADJUSTMENT")
    RESCHEDULE_ADJUSTMENT;

    val isCredit: Boolean
        get() = this == TOP_UP || this == REFUND

    val displayLabel: String
        get() = when (this) {
            TOP_UP -> "Top Up"
            APPOINTMENT_PAYMENT -> "Appointment Payment"
            REFUND -> "Refund"
            RESCHEDULE_ADJUSTMENT -> "Reschedule Adjustment"
        }
}

@Serializable
data class PaymentMethodSummary(
    @SerialName("card_brand")
    val cardBrand: String? = null,

    @SerialName("last4_digits")
    val last4Digits: String? = null,

    @SerialName("type")
    val type: String? = null
) {
    val displayTitle: String
        get() = if (!cardBrand.isNullOrBlank() && !last4Digits.isNullOrBlank()) {
            "$cardBrand •••• $last4Digits"
        } else if (!cardBrand.isNullOrBlank()) {
            cardBrand
        } else if (!type.isNullOrBlank()) {
            type
        } else {
            "Card"
        }
}

@Serializable
data class WalletTransaction(
    @SerialName("id")
    val id: String = "",

    @SerialName("wallet_id")
    val walletId: String = "",

    @SerialName("payment_id")
    val paymentId: String? = null,

    @SerialName("paymentMethod_id")
    val paymentMethodId: String? = null,

    @SerialName("payment_method")
    val paymentMethod: PaymentMethodSummary? = null,

    @SerialName("transaction_type")
    val transactionType: String = "",

    @SerialName("amount")
    val amount: Double = 0.0,

    @SerialName("balance_before")
    val balanceBefore: Double = 0.0,

    @SerialName("balance_after")
    val balanceAfter: Double = 0.0,

    @SerialName("description")
    val description: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    val typeEnum: WalletTransactionType
        get() = try {
            WalletTransactionType.valueOf(transactionType)
        } catch (_: Exception) {
            if (transactionType.contains("TOP_UP", ignoreCase = true)) WalletTransactionType.TOP_UP
            else if (transactionType.contains("REFUND", ignoreCase = true)) WalletTransactionType.REFUND
            else if (transactionType.contains("RESCHEDULE", ignoreCase = true)) WalletTransactionType.RESCHEDULE_ADJUSTMENT
            else WalletTransactionType.APPOINTMENT_PAYMENT
        }

    val isCredit: Boolean
        get() = typeEnum == WalletTransactionType.TOP_UP || typeEnum == WalletTransactionType.REFUND || (typeEnum == WalletTransactionType.RESCHEDULE_ADJUSTMENT && balanceAfter > balanceBefore)

    val displayDescription: String
        get() {
            if (typeEnum == WalletTransactionType.TOP_UP) {
                paymentMethod?.let { pm ->
                    return "Top-up via ${pm.displayTitle}"
                }
            }
            return description ?: typeEnum.displayLabel
        }
}

package com.example.foodieheal.wallet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
data class PaymentMethodRecord(
    @SerialName("payment_method_id")
    val paymentMethodId: String = "",

    @SerialName("card_brand")
    val cardBrand: String? = null,

    @SerialName("last4_digits")
    val last4Digits: String? = null,

    @SerialName("type")
    val type: String? = null
) {
    fun toSummary(): PaymentMethodSummary = PaymentMethodSummary(
        cardBrand = cardBrand,
        last4Digits = last4Digits,
        type = type
    )
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

    @SerialName("transaction_type")
    val transactionType: String = "",

    @SerialName("amount")
    val amount: Double? = 0.0,

    @SerialName("balance_before")
    val balanceBefore: Double? = 0.0,

    @SerialName("balance_after")
    val balanceAfter: Double? = 0.0,

    @SerialName("description")
    val description: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @Transient
    val paymentMethod: PaymentMethodSummary? = null
) {
    val safeAmount: Double
        get() = amount ?: 0.0

    val safeBalanceBefore: Double
        get() = balanceBefore ?: 0.0

    val safeBalanceAfter: Double
        get() = balanceAfter ?: 0.0

    val typeEnum: WalletTransactionType
        get() = try {
            WalletTransactionType.valueOf(transactionType.trim().uppercase())
        } catch (_: Exception) {
            val upper = transactionType.trim().uppercase()
            if (upper.contains("TOP_UP") || upper.contains("TOPUP") || upper.contains("TOP UP")) WalletTransactionType.TOP_UP
            else if (upper.contains("REFUND")) WalletTransactionType.REFUND
            else if (upper.contains("RESCHEDULE")) WalletTransactionType.RESCHEDULE_ADJUSTMENT
            else WalletTransactionType.APPOINTMENT_PAYMENT
        }

    val isCredit: Boolean
        get() = typeEnum == WalletTransactionType.TOP_UP || typeEnum == WalletTransactionType.REFUND || (typeEnum == WalletTransactionType.RESCHEDULE_ADJUSTMENT && safeBalanceAfter >= safeBalanceBefore)

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

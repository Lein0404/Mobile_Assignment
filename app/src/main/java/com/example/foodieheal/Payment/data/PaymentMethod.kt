package com.example.foodieheal.Payment.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodEntity(
    @SerialName("payment_method_id")
    val paymentMethodId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("type")
    val type: String,

    @SerialName("card_brand")
    val cardBrand: String? = null,

    @SerialName("last4_digits")
    val last4Digits: String? = null,

    @SerialName("expiry_date")
    val expiryDate: String? = null,

    @SerialName("is_default")
    val isDefault: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null
)

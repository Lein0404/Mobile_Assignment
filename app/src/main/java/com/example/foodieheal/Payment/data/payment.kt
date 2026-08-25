package com.example.foodieheal.Payment.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class payment(
    @SerialName("paymentId")
    val paymentId: String,

    @SerialName("transactionId")
    val transactionId : String? = null,

    @SerialName("appointmentID")
    val appointmentId: String,

    @SerialName("userId")
    val userId: String,

    @SerialName("totalAmount")
    val totalAmount: Double,

    @SerialName("paymentMethod")
    val paymentMethod: String? = null,

    @SerialName("paymentMethod_id")
    val paymentMethodId: String? = null,

    @SerialName("status")
    val status: String,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("pay_at")
    val payAt: String? = null
)

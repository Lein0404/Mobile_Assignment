package com.example.foodieheal.Payment.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class payment(
    @SerialName("paymentId")
    val paymentId: String,

    @SerialName("transactionId")
    val transactionId : String,

    @SerialName("appointmentID")
    val appointmentId: String,

    @SerialName("userId")
    val userId: String,

    @SerialName("totalAmount")
    val totalAmount: Double,

    @SerialName("paymentMethod")
    val paymentMethod: String,

    @SerialName("paymentMethod_id")
    val paymentMethodId: String? = null,

    @SerialName("status")
    val status: String
)

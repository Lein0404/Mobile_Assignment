package com.example.foodieheal.wallet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Wallet(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("balance")
    val balance: Double = 0.0,

    @SerialName("is_Active")
    val isActive: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("update_at")
    val updateAt: String? = null
)

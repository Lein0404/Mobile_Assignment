package com.example.foodieheal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Centralized status definition for Chef Approval and Ingredient Request modules

@Serializable
enum class Status(val statusName: String) {
    @SerialName("APPROVED") APPROVED("Approved"),
    @SerialName("PENDING") PENDING("Pending"),
    @SerialName("REJECTED") REJECTED("Rejected")
}
package com.example.foodieheal.model

// Centralized status definition for Chef Approval and Ingredient Request modules

enum class Status(val statusName: String) {
    APPROVED("Approved"),
    PENDING("Pending"),
    REJECTED("Rejected")
}
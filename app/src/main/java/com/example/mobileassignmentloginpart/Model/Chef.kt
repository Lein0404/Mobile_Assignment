package com.example.mobileassignmentloginpart.Model

data class Chef(
    val chefId: String = "",
    val id: String? = "",
    val password: String ="",

    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val gender: String = "",
    val age: String = "",

    val address: String = "",
    val postcode: String = "",
    val state: String = "",

    val experience: Int = 0,
    val description: String = "",

    val profileImageUrl: String = "",
    // val certificateUrl: String = "",

    val location: String = "",

    val approvalStatus: String = "Pending",
    val rating: Double = 0.0,
    val totalBooking: Int = 0
)
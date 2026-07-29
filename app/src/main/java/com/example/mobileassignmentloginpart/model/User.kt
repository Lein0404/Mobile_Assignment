package com.example.mobileassignmentloginpart.model

data class User(
    val id: String = "",
    val customId: String = "", // Added for U00001 format
    val email: String = "",
    val name: String = "",
    val profilePicUrl: String = ""
)

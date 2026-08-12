package com.example.foodieheal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String? = "",
    @SerialName("custom_id")
    val customId: String? = "",
    val email: String? = "",
    val name: String? = "",
    @SerialName("profile_pic_url")
    val profilePicUrl: String? = "",
    @SerialName("user_description")
    val description: String? = "",
    val weight: Double? = 0.0,
    val height: Double? = 0.0,
    val age: Int? = 0,
    val gender: String? = "",
    val bmi: Double? = 0.0
)

package com.example.foodieheal.Model

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
    val profilePicUrl: String? = ""
)

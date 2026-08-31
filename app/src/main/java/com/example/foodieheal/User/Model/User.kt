package com.example.foodieheal.User.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//serializable means it can be converted to/from JSON data
//the name inside the serial name need to match the supabase table column
@Serializable
data class User(
    @SerialName("id") val id: String? = "",
    @SerialName("custom_id") val customId: String? = "",
    @SerialName("email") val email: String? = "",
    @SerialName("name") val name: String? = "",
    @SerialName("profile_pic_url") val profilePicUrl: String? = "",
    @SerialName("user_description") val description: String? = "",
    @SerialName("weight") val weight: Double? = 0.0,
    @SerialName("height") val height: Double? = 0.0,
    @SerialName("age") val age: Int? = 0,
    @SerialName("gender") val gender: String? = "",
    @SerialName("bmi") val bmi: Double? = 0.0,
    @SerialName("is_private") val isPrivate: Boolean? = false,
    @SerialName("follower_count") val followerCount: Int? = 0,
    @SerialName("following_count") val followingCount: Int? = 0
)
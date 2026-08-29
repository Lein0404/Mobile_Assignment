package com.example.foodieheal.User.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Follow(
    @SerialName("id") val id: String? = null,
    @SerialName("follower_id") val followerId: String? = "",
    @SerialName("following_id") val followingId: String? = "",
    @SerialName("status") val status: String = "PENDING",
    @SerialName("created_at") val createdAt: String? = null
)

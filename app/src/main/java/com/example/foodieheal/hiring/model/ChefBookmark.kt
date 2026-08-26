package com.example.foodieheal.hiring.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChefBookmark(
    @SerialName("bookmarkId")
    val id: String? = null,
    @SerialName("id")
    val userId: String,
    @SerialName("chefId")
    val chefId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)
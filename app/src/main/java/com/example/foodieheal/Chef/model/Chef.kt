package com.example.foodieheal.Chef.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chef(
    @SerialName("chefId")
    val chefId: String,

    @SerialName("id")
    val id : String,

    @SerialName("Name")
    val name: String,

    @SerialName("gender")
    val gender: String,

    @SerialName("age")
    val age: Int,

    @SerialName("phoneNumber")
    val phoneNumber: String,

    @SerialName("email")
    val email: String,

    @SerialName("address")
    val address: String,

    @SerialName("state")
    val state: String,

    @SerialName("postcode")
    val postcode: String,

    @SerialName("experience")
    val experience: Int,

    @SerialName("description")
    val description: String,

    @SerialName("profilePictureUrl")
    val profilePictureUrl: String? = null,

    @SerialName("averagerating")
    val averagerating: Double ? = null,

    @SerialName("Pricing")
    val Pricing : Double ? =null,

    @SerialName("Status")
    val status: String
)
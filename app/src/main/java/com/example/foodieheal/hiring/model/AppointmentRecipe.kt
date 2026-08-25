package com.example.foodieheal.hiring.model

import com.example.foodieheal.Recipe.Model.Recipe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentRecipe(
    @SerialName("id")
    val id: String? = null,

    @SerialName("appointmentId")
    val appointmentId: String,

    @SerialName("recipeId")
    val recipeId: String,

    @SerialName("service_count")
    val service_count: Double = 2.0,

    @SerialName("custom_note")
    val custom_note: String? = null
)

@Serializable
data class AppointmentRecipeWithDetails(
    @SerialName("id")
    val id: String? = null,

    @SerialName("appointmentId")
    val appointmentId: String = "",

    @SerialName("recipeId")
    val recipeId: String = "",

    @SerialName("service_count")
    val service_count: Double = 2.0,

    @SerialName("custom_note")
    val custom_note: String? = null,

    val recipe: Recipe? = null
)

data class SelectedAppointmentRecipe(
    val recipe: Recipe,
    val serviceCount: Int = 2,
    val customNote: String = ""
)

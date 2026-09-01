package com.example.foodieheal.hiring.model

import com.example.foodieheal.Recipe.Model.Recipe
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AppointmentRecipe(
    @SerialName("id")
    val id: String? = null,

    @SerialName("appointmentId")
    val appointmentId: String,

    @SerialName("recipeId")
    val recipeId: String,

    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("service_count")
    val service_count: Double = 1.0,

    @SerialName("custom_note")
    val custom_note: String? = null,

    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("chef_provide_ingredient")
    val chef_provide_ingredient: Boolean = true
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
    val service_count: Double = 1.0,

    @SerialName("custom_note")
    val custom_note: String? = null,

    @SerialName("chef_provide_ingredient")
    val chef_provide_ingredient: Boolean = true,

    val recipe: Recipe? = null
)

data class SelectedAppointmentRecipe(
    val recipe: Recipe,
    val serviceCount: Int = 1,
    val customNote: String = "",
    val chefProvidesIngredients: Boolean = true
)

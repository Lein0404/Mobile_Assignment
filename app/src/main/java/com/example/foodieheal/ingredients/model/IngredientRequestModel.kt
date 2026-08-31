package com.example.foodieheal.ingredients.model

import com.example.foodieheal.model.Status
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IngredientRequest(
    @SerialName("ingredient_request_id") val ingredientRequestId: String,
    @SerialName("ing_name") val ingredientName: String,
    @SerialName("ing_category") val ingredientCategory: IngredientCategory? = null,
    @SerialName("ing_description") val ingredientDesc: String = "",
    @SerialName("ing_image") val ingredientImage: String? = null,
    @SerialName("user_id") val createdByUserId: String,
    @SerialName("request_status") val requestStatus: Status,
    @SerialName("rejected_reason") val rejectedReason: String? = null,
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("ingredient_id") val ingredientId: String? = null,
    @SerialName("datetime_created") val datetimeCreated: String? = null,
    @SerialName("datetime_processed") val datetimeProcessed: String? = null
)

@Serializable
data class IngredientUnitsRequest(
    @SerialName("ingredient_units_request_id") val ingredientUnitsRequestId: String = "",
    @SerialName("ingredient_request_id") val ingredientRequestId: String = "",
    @SerialName("unit_id") val unitID: String = "",
    @SerialName("calories_per_default_quantity") val caloriesPerDefaultQuantity: Double = 0.0,
)

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
    @SerialName("datetime_created") val datetimeCreated: String? = null
)

@Serializable
data class IngredientUnitsRequest(
    @SerialName("ingredient_units_request_id") val ingredientUnitsRequestId: String = "",
    @SerialName("ingredient_request_id") val ingredientRequestId: String = "",
    @SerialName("unit_id") val unitID: String = "",
    @SerialName("calories_per_default_quantity") val caloriesPerDefaultQuantity: Double = 0.0,
)

data class IngredientRequestItem(
    val request: IngredientRequest,
    val calorieSummary: String = ""
)

data class IngredientRequestUiState(
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val requests: List<IngredientRequestItem> = emptyList(),
    val filteredRequests: List<IngredientRequestItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isNetworkAvailable: Boolean = true
)

data class IngredientRequestFormUiState(
    val requestId: String? = null,
    val ingredientName: String = "",
    val category: IngredientCategory? = null,
    val description: String = "",
    val imageUrl: String? = null,
    val unitRows: List<UnitRowState> = listOf(UnitRowState()),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    
    // Per-field validation errors
    val nameError: String? = null,
    val categoryError: String? = null,
    val descriptionError: String? = null,
    val unitRowsError: String? = null,
)

data class UnitRowState(
    val selectedUnit: Units? = null,
    val calories: String = "",
    val unitError: String? = null,
    val caloriesError: String? = null,
)

package com.example.foodieheal.ingredients.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ingredients(
    @SerialName("ingredient_id") val ingredientId: String = "",
    @SerialName("ing_name") val ingredientName: String = "",
    @SerialName("ing_category") val ingredientCategory: IngredientCategory? = null,
    @SerialName("ing_description") val ingredientDesc: String = "",
    @SerialName("ing_image") val ingredientImage: String? = null,
    @SerialName("created_by_user_id") val createdByUserId: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
)

@Serializable
data class IngredientUnits(
    @SerialName("ingredient_unit_id") val ingredientUnitId: String = "",
    @SerialName("ingredient_id") val ingredientID: String = "",
    @SerialName("unit_id") val unitID: String = "",
    @SerialName("calories_per_default_quantity") val caloriesPerDefaultQuantity: Double = 0.0,
)

@Serializable
data class Units(
    @SerialName("unit_id") val unitID: String = "",
    @SerialName("unit_name") val unitName: String = "",
    @SerialName("unit_display") val unitDisplay: String = "",
    @SerialName("default_quantity") val defaultQuantity: Double = 1.0
)

@Serializable
enum class IngredientCategory(val categoryName: String){
    @SerialName("BAKERY") BAKERY("Bakery"),
    @SerialName("BEVERAGES") BEVERAGES("Beverages"),
    @SerialName("BREAKFAST_CEREALS") BREAKFAST_CEREALS("Breakfast & Cereals"),
    @SerialName("CANNED_PACKAGED") CANNED_PACKAGED("Canned & Packaged"),
    @SerialName("CONDIMENTS_SAUCES_DRESSINGS") CONDIMENTS_SAUCES_DRESSINGS("Condiments, Sauces & Dressings"),
    @SerialName("DAIRY_EGGS") DAIRY_EGGS("Dairy & Eggs"),
    @SerialName("FROZEN_FOODS") FROZEN_FOODS("Frozen Foods"),
    @SerialName("FRUITS") FRUITS("Fruits"),
    @SerialName("GRAINS_RICE") GRAINS_RICE("Grains & Rice"),
    @SerialName("HERBS_SPICES_SEASONINGS") HERBS_SPICES_SEASONINGS("Herbs, Spices & Seasonings"),
    @SerialName("LEGUMES_BEANS") LEGUMES_BEANS("Legumes & Beans"),
    @SerialName("MEAT_POULTRY") MEAT_POULTRY("Meat & Poultry"),
    @SerialName("NUTS_SEEDS") NUTS_SEEDS("Nuts & Seeds"),
    @SerialName("OILS_FATS") OILS_FATS("Oils & Fats"),
    @SerialName("SEAFOOD") SEAFOOD("Seafood"),
    @SerialName("SNACKS_SWEETS") SNACKS_SWEETS("Snack & Sweets"),
    @SerialName("VEGETABLES") VEGETABLES("Vegetables"),
    @SerialName("OTHERS") OTHERS("Others")
}

data class IngredientItem(
    val ingredient: Ingredients,
    val calorieSummary: String = ""
)

data class IngredientDetailInfo(
    val ingredient: Ingredients,
    val calorieEntries: List<CalorieEntry> = emptyList(),
    val calorieSummary: String = ""
)

data class CalorieEntry(
    val calories: Double,
    val quantity: Double,
    val unitName: String
)

data class IngredientsUiState(
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val ingredients: List<IngredientItem> = emptyList(),
    val filteredIngredients: List<IngredientItem> = emptyList(),
    val isLoading: Boolean = false,
    val ingredientDetail: IngredientDetailInfo? = null,
    val errorMessage: String? = null
)
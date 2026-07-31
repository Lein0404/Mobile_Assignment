package com.example.foodieheal.ingredients.model

data class Ingredients(
    val ingredientId: String = "",
    val ingredientName: String = "",
    val ingredientCategory: IngredientCategory? = null,
    val ingredientDesc: String = "",
    val createdByUserId: String? = null,
    val isDefault: Boolean = false
)

data class IngredientUnits(
    val ingredientUnitId: String = "",
    val ingredientID: String = "",
    val unitID: String = "",
    val caloriesPerDefaultQuantity: Double = 0.0,
)

data class Units(
    val unitID: String = "",
    val unitName: String = "",
    val defaultQuantity: Double = 1.0
)

enum class IngredientCategory(val categoryName: String){
    BAKERY("Bakery"),
    BEVERAGES("Beverages"),
    BREAKFAST_CEREALS("Breakfast & Cereals"),
    CANNED_PACKAGED("Canned & Packaged"),
    CONDIMENTS_SAUCES_DRESSINGS("Condiments, Sauces & Dressings"),
    DAIRY_EGGS("Diary & Eggs"),
    FROZEN_FOODS("Frozen Foods"),
    FRUITS("Fruits"),
    GRAINS_RICE("Grains & Rice"),
    HERBS_SPICES_SEASONINGS("Herbs, Spices & Seasonings"),
    LEGUMES_BEANS("Legumes & Beans"),
    MEAT_POULTRY("Meat & Poultry"),
    NUTS_SEEDS("Nuts & Seeds"),
    OILS_FATS("Oils & Fats"),
    SEAFOOD("Seafood"),
    SNACKS_SWEETS("Snack & Sweets"),
    VEGETABLES("Vegetables"),
    OTHERS("Others")
}
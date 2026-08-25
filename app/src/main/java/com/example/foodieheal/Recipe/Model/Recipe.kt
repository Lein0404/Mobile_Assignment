package com.example.foodieheal.Recipe.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    @SerialName("recipe_id") val recipe_id: String? = null,
    @SerialName("recipe_author") val author_id: String? = null,
    @SerialName("recipe_name") val recipeName: String,
    @SerialName("recipe_description") val recipeDescription: String,
    @SerialName("recipe_course") val recipeCourse: String, 
    @SerialName("recipe_time") val time: Int, 
    @SerialName("recipe_calories") val calories: Int,
    @SerialName("cooking_skill") val cookingSkill: String, 
    @SerialName("estimated_budget") val estimatedBudget: String,
    @SerialName("recipe_steps") val recipeStep: String,
    @SerialName("recipe_image") val recipeImageUrl: String? = null,
    @SerialName("recipe_ingredients") val ingredients: List<IngredientItem>,
    @SerialName("last_updated") val lastUpdated: String? = null // 🌟 New column
)

@Serializable
data class IngredientItem(
    val name: String,
    val quantity: String,
    val unit: String
)

@Serializable
data class Ingredient(
    @SerialName("ingredient_unit_id") val id: String? = null,
    @SerialName("ing_name") val name: String? = "",
    @SerialName("calories_per_default_quantity") val kcal: Double? = 0.0,
    @SerialName("unit_name") val defaultUnit: String? = null,
    @SerialName("units") val unitDetails: UnitDetails? = null // 🌟 Joined data from units table
) {
    // 🌟 Helper to get default quantity easily
    val defaultQuantity: Double get() = unitDetails?.defaultQuantity ?: 1.0
}

@Serializable
data class UnitDetails(
    @SerialName("default_quantity") val defaultQuantity: Double = 1.0
)

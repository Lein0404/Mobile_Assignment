package com.example.foodieheal.Recipe.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class Recipe(
    @SerialName("recipe_id") val recipe_id: String? = null,
    @SerialName("recipe_author") val author_id: String? = null,
    @SerialName("recipe_name") val recipeName: String = "Loading...",
    @SerialName("recipe_description") val recipeDescription: String = "",
    @SerialName("recipe_course") val recipeCourse: String = "",
    @SerialName("recipe_time") val time: Int = 0,
    @SerialName("recipe_calories") val calories: Int = 0,
    @SerialName("cooking_skill") val cookingSkill: String = "",
    @SerialName("estimated_budget") val estimatedBudget: String = "",
    @SerialName("recipe_steps") val recipeStep: String = "",
    @SerialName("recipe_image") val recipeImageUrl: String? = null,
    @SerialName("recipe_ingredients") val ingredients: List<IngredientItem> = emptyList(),
    @SerialName("last_updated") val lastUpdated: String? = null,
    
    // 🌟 Join result field: catches the author name/pic during decoding.
    @SerialName("users") var authorInfo: AuthorInfo? = null,

    @kotlinx.serialization.Transient var authorName: String? = null,
    @kotlinx.serialization.Transient var authorImageUrl: String? = null
)

@Serializable
data class AuthorInfo(
    @SerialName("name") val name: String? = null,
    @SerialName("profile_pic_url") val profile_pic_url: String? = null
)

@Serializable
data class IngredientItem(
    @SerialName("name") val name: String = "",
    // 🌟 Use JsonElement to handle both String ("2") and Number (2) from Supabase safely
    @SerialName("quantity") val quantity: JsonElement = JsonPrimitive("0"),
    @SerialName("unit") val unit: String = ""
) {
    val displayQuantity: String get() = try {
        when (val q = quantity) {
            is JsonPrimitive -> q.content
            else -> q.toString()
        }
    } catch (e: Exception) {
        "0"
    }
}

@Serializable
data class Ingredient(
    @SerialName("ingredient_unit_id") val id: String? = null,
    @SerialName("ing_name") val name: String? = "",
    @SerialName("calories_per_default_quantity") val kcal: Double? = 0.0,
    @SerialName("unit_name") val defaultUnit: String? = null,
    @SerialName("units") val unitDetails: UnitDetails? = null 
) {
    val defaultQuantity: Double get() = unitDetails?.defaultQuantity ?: 1.0
}

@Serializable
data class UnitDetails(
    @SerialName("default_quantity") val defaultQuantity: Double = 1.0
)

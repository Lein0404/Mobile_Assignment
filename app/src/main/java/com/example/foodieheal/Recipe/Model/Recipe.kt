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
    @SerialName("visibility") val visibility: String = "public",

    // 🌟 Join result field: catches the author name/pic during decoding if columns are missing.
    @SerialName("users") var authorInfo: AuthorInfo? = null,

    // 🌟 Denormalized fields: Stored directly in 'recipes' table for speed and offline reliability.
    @SerialName("author_name") var authorName: String? = null,
    @SerialName("author_image_url") var authorImageUrl: String? = null,
    @SerialName("author_image_cache") var authorImageCache: String? = null,

    @kotlinx.serialization.Transient var isOffline: Boolean = false
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
    } catch (_: Exception) {
        "0"
    }
}

@Serializable
data class Ingredient(
    @SerialName("ingredient_unit_id") val id: String? = null,
    @SerialName("ing_name") val name: String? = "",
    @SerialName("ing_description") val description: String? = null,
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

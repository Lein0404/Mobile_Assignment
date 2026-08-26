package com.example.foodieheal.Recipe.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "local_recipes")
data class RecipeEntity(
    @PrimaryKey val recipe_id: String,
    val author_id: String?,
    val recipeName: String,
    val recipeDescription: String,
    val recipeCourse: String,
    val time: Int,
    val calories: Int,
    val cookingSkill: String,
    val estimatedBudget: String,
    val recipeStep: String,
    val recipeImageUrl: String?,
    val ingredientsJson: String,
    val lastUpdated: String?, // 🌟 New column
    val authorName: String? = null, // 🌟 Cache author info for offline
    val authorImageUrl: String? = null
)

fun RecipeEntity.toDomain(json: kotlinx.serialization.json.Json): com.example.foodieheal.Recipe.Model.Recipe {
    return com.example.foodieheal.Recipe.Model.Recipe(
        recipe_id = this.recipe_id,
        author_id = this.author_id,
        recipeName = this.recipeName,
        recipeDescription = this.recipeDescription,
        recipeCourse = this.recipeCourse,
        time = this.time,
        calories = this.calories,
        cookingSkill = this.cookingSkill,
        estimatedBudget = this.estimatedBudget,
        recipeStep = this.recipeStep,
        recipeImageUrl = this.recipeImageUrl,
        ingredients = try {
            json.decodeFromString<List<com.example.foodieheal.Recipe.Model.IngredientItem>>(this.ingredientsJson)
        } catch (e: Exception) {
            emptyList()
        },
        lastUpdated = this.lastUpdated,
        authorName = this.authorName,
        authorImageUrl = this.authorImageUrl
    )
}

fun com.example.foodieheal.Recipe.Model.Recipe.toEntity(json: kotlinx.serialization.json.Json): RecipeEntity {
    return RecipeEntity(
        recipe_id = this.recipe_id ?: "",
        author_id = this.author_id,
        recipeName = this.recipeName,
        recipeDescription = this.recipeDescription,
        recipeCourse = this.recipeCourse,
        time = this.time,
        calories = this.calories,
        cookingSkill = this.cookingSkill,
        estimatedBudget = this.estimatedBudget,
        recipeStep = this.recipeStep,
        recipeImageUrl = this.recipeImageUrl,
        ingredientsJson = json.encodeToString(this.ingredients),
        lastUpdated = this.lastUpdated,
        authorName = this.authorInfo?.name ?: this.authorName,
        authorImageUrl = this.authorInfo?.profile_pic_url ?: this.authorImageUrl
    )
}

@Entity(tableName = "local_ingredients")
data class IngredientEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val kcal: Double?,
    val defaultUnit: String?,
    val defaultQuantity: Double? = 1.0 // 🌟 New field
)

package com.example.foodieheal.Recipe.local

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val lastUpdated: String? // 🌟 New column
)

@Entity(tableName = "local_ingredients")
data class IngredientEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val kcal: Double?,
    val defaultUnit: String?,
    val defaultQuantity: Double? = 1.0 // 🌟 New field
)

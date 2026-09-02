package com.example.foodieheal.Recipe.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodieheal.Recipe.Model.IngredientItem
import com.example.foodieheal.Recipe.Model.Recipe
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
    val lastUpdated: String?,
    val visibility: String = "public",
    val authorName: String? = null,
    val authorImageUrl: String? = null
)

fun RecipeEntity.toDomain(json:Json): Recipe {
    return Recipe(
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
            json.decodeFromString<List<IngredientItem>>(this.ingredientsJson)
        } catch (e: Exception) {
            emptyList()
        },
        lastUpdated = this.lastUpdated,
        visibility = this.visibility,
        authorName = this.authorName,
        authorImageUrl = this.authorImageUrl
    )
}

fun Recipe.toEntity(json: Json): RecipeEntity {
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
        visibility = this.visibility,
        authorName = this.authorInfo?.name ?: this.authorName,
        authorImageUrl = this.authorInfo?.profile_pic_url ?: this.authorImageUrl
    )
}

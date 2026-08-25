package com.example.foodieheal.Recipe.local

import androidx.room.Entity

@Entity(tableName = "local_bookmarks", primaryKeys = ["userId", "recipeId"])
data class RecipeBookmarkEntity(
    val userId: String,
    val recipeId: String
)
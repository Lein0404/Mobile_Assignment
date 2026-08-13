package com.example.foodieheal.database

import androidx.room.Entity

@Entity(tableName = "local_bookmarks", primaryKeys = ["userId", "recipeId"])
data class BookmarkEntity(
    val userId: String,
    val recipeId: String
)

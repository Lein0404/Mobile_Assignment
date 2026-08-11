package com.example.foodieheal.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecipeDao {
    @Query("SELECT * FROM local_recipes")
    suspend fun getAllRecipes(): List<RecipeEntity>

    @Query("SELECT * FROM local_recipes WHERE author_id = :authorId")
    suspend fun getMyRecipes(authorId: String): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Query("DELETE FROM local_recipes")
    suspend fun clearRecipes()

    @Query("SELECT * FROM local_ingredients")
    suspend fun getAllIngredients(): List<IngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Query("DELETE FROM local_ingredients")
    suspend fun clearIngredients()

    @Query("SELECT recipeId FROM local_bookmarks WHERE userId = :userId")
    suspend fun getBookmarkIds(userId: String): List<String>

    @Query("SELECT * FROM local_recipes INNER JOIN local_bookmarks ON local_recipes.recipe_id = local_bookmarks.recipeId WHERE local_bookmarks.userId = :userId")
    suspend fun getBookmarkedRecipes(userId: String): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM local_bookmarks WHERE userId = :userId")
    suspend fun clearBookmarks(userId: String)

    @Query("SELECT * FROM local_recipes WHERE recipe_id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: String): RecipeEntity?
}

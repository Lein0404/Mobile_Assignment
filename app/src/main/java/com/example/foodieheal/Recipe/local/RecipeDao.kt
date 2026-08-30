package com.example.foodieheal.Recipe.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns

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

    @Query("SELECT recipeId FROM local_bookmarks WHERE userId = :userId")
    suspend fun getBookmarkIds(userId: String): List<String>

    @Query("SELECT local_recipes.* FROM local_recipes INNER JOIN local_bookmarks ON local_recipes.recipe_id = local_bookmarks.recipeId WHERE local_bookmarks.userId = :userId")
    suspend fun getBookmarkedRecipes(userId: String): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<RecipeBookmarkEntity>)

    @Query("DELETE FROM local_bookmarks WHERE userId = :userId AND recipeId = :recipeId")
    suspend fun deleteBookmark(userId: String, recipeId: String)

    @Query("DELETE FROM local_bookmarks WHERE userId = :userId")
    suspend fun clearBookmarks(userId: String)

    @Query("DELETE FROM local_recipes WHERE recipe_id = :recipeId")
    suspend fun deleteRecipe(recipeId: String)

    @Query("SELECT * FROM local_recipes WHERE recipe_id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: String): RecipeEntity?

    @Query("SELECT * FROM local_recipes WHERE recipe_id IN (:recipeIds)")
    suspend fun getRecipesByIds(recipeIds: List<String>): List<RecipeEntity>
}
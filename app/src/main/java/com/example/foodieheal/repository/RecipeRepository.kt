package com.example.foodieheal.repository

import com.example.foodieheal.model.Recipe
import com.example.foodieheal.model.Ingredient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeRepository(
    private val supabaseClient: SupabaseClient
) {
    suspend fun getAllRecipes(): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = supabaseClient.postgrest.from("recipes").select()
            response.decodeList<Recipe>()
        }
    }

    suspend fun getMyRecipes(authorId: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = supabaseClient.postgrest.from("recipes")
                .select { filter { eq("author_id", authorId) } }
            response.decodeList<Recipe>()
        }
    }

    suspend fun insertRecipe(recipe: Recipe): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabaseClient.postgrest.from("recipes").insert(recipe)
            Unit
        }
    }

    suspend fun uploadRecipeImage(recipeId: String, imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bucket = supabaseClient.storage.from("recipes")
            val fileName = "recipe_$recipeId.jpg"
            bucket.upload(fileName, imageBytes) {
                upsert = true
            }
            bucket.publicUrl(fileName)
        }
    }

    suspend fun getAvailableIngredients(): Result<List<Ingredient>> = withContext(Dispatchers.IO) {
        runCatching {
            supabaseClient.postgrest.from("ingredient_units").select().decodeList<Ingredient>()
        }
    }

    // --- Bookmark Functions ---

    suspend fun toggleBookmark(userId: String, recipeId: String, isBookmarked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (isBookmarked) {
                // Remove bookmark
                supabaseClient.postgrest.from("recipe_bookmarks").delete {
                    filter {
                        eq("user_id", userId)
                        eq("recipe_id", recipeId)
                    }
                }
            } else {
                // Add bookmark
                val data = mapOf("user_id" to userId, "recipe_id" to recipeId)
                supabaseClient.postgrest.from("recipe_bookmarks").insert(data)
            }
            Unit
        }
    }

    suspend fun getBookmarkedRecipes(userId: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            // Fetch bookmarks and join with recipes table
            val response = supabaseClient.postgrest.from("recipe_bookmarks")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("recipes(*)")) {
                    filter { eq("user_id", userId) }
                }
            response.decodeList<BookmarkJoin>().map { it.recipes }
        }
    }

    suspend fun getUserBookmarkIds(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = supabaseClient.postgrest.from("recipe_bookmarks")
                .select(io.github.jan.supabase.postgrest.query.Columns.list("recipe_id")) {
                    filter { eq("user_id", userId) }
                }
            // Return just the list of IDs for quick UI checking
            response.decodeList<BookmarkId>().map { it.recipe_id }
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabaseClient.postgrest.from("recipes").delete {
                filter { eq("recipe_id", recipeId) }
            }
            Unit
        }
    }

    suspend fun getRecipeById(recipeId: String): Result<Recipe> = withContext(Dispatchers.IO) {
        runCatching {
            val response = supabaseClient.postgrest.from("recipes")
                .select {
                    filter { eq("recipe_id", recipeId) }
                }
            // decodeSingle() handles throwing an exception if the row doesn't exist
            response.decodeSingle<Recipe>()
        }
    }

    fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
}


@kotlinx.serialization.Serializable
data class BookmarkJoin(val recipes: Recipe)

@kotlinx.serialization.Serializable
data class BookmarkId(val recipe_id: String)

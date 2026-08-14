package com.example.foodieheal.repository

import com.example.foodieheal.model.Recipe
import com.example.foodieheal.model.Ingredient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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
                .select { filter { eq("recipe_author", authorId) } }
            response.decodeList<Recipe>()
        }
    }

    suspend fun insertRecipe(recipe: Recipe): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabaseClient.postgrest.from("recipes").insert(recipe)
            Unit
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabaseClient.postgrest.from("recipes").update(recipe) {
                filter { eq("recipe_id", recipe.recipe_id ?: "") }
            }
            Unit
        }
    }

    suspend fun uploadRecipeImage(recipeId: String, imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 🌟 Updated to use Cloudinary with modern OkHttp standards
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", com.example.foodieheal.Cloudinary.CloudinaryConfig.UPLOAD_PRESET)
                .addFormDataPart("file", "recipe_$recipeId.jpg", imageBytes.toRequestBody("image/*".toMediaType()))
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/${com.example.foodieheal.Cloudinary.CloudinaryConfig.CLOUD_NAME}/image/upload")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Cloudinary failed: ${response.message}")
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                json.getString("secure_url")
            }
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
            val table = supabaseClient.postgrest.from("recipe_bookmarks")
            if (isBookmarked) {
                table.delete {
                    filter {
                        eq("user_id", userId)
                        eq("recipe_id", recipeId)
                    }
                }
            } else {
                val data = mapOf(
                    "user_id" to userId,
                    "recipe_id" to recipeId
                )
                table.insert(data)
            }
            Unit
        }
    }

    suspend fun getBookmarkedRecipes(userId: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            val idResponse = supabaseClient.postgrest.from("recipe_bookmarks")
                .select(io.github.jan.supabase.postgrest.query.Columns.list("recipe_id")) {
                    filter { eq("user_id", userId) }
                }
            val bookmarkedIds = idResponse.decodeList<BookmarkId>().map { it.recipe_id }

            if (bookmarkedIds.isEmpty()) return@runCatching emptyList<Recipe>()

            val recipeResponse = supabaseClient.postgrest.from("recipes")
                .select {
                    filter {
                        isIn("recipe_id", bookmarkedIds)
                    }
                }
            recipeResponse.decodeList<Recipe>()
        }
    }

    suspend fun getUserBookmarkIds(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = supabaseClient.postgrest.from("recipe_bookmarks")
                .select(io.github.jan.supabase.postgrest.query.Columns.list("recipe_id")) {
                    filter { eq("user_id", userId) }
                }
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

@kotlinx.serialization.Serializable
data class RecipeBookmark(
    val user_id: String,
    val recipe_id: String
)

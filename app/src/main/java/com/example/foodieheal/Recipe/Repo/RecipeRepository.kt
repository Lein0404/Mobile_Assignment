package com.example.foodieheal.Recipe.Repo

import android.util.Log
import com.example.foodieheal.Cloudinary.CloudinaryConfig
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.local.RecipeEntity
import com.example.foodieheal.Recipe.local.toDomain
import com.example.foodieheal.Recipe.local.toEntity
import com.example.foodieheal.Recipe.Model.IngredientItem
import com.example.foodieheal.SupabaseClient.client
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RecipeRepository(
    private val recipeDao: com.example.foodieheal.Recipe.local.RecipeDao? = null
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAllRecipes(): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val recipes = try {
                    client.from("recipes")
                        .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)"))
                        .decodeList<Recipe>()
                } catch (e: Exception) {
                    Log.d("RecipeRepository", "AllRecipes Join Fallback Triggered: ${e.localizedMessage}")
                    client.from("recipes").select().decodeList<Recipe>()
                }

                // 🌟 FIX: Flatten author info immediately so UI and Local DB can see it
                recipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }

                recipeDao?.let { dao ->
                    dao.clearRecipes()
                    dao.insertRecipes(recipes.map { it.toEntity(json) })
                }
                
                recipes
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error fetching all recipes", e)
                recipeDao?.getAllRecipes()?.map { it.toDomain(json) } ?: emptyList()
            }
        }
    }

    suspend fun getMyRecipes(authorId: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val response = client.from("recipes")
                    .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                        filter { eq("recipe_author", authorId) }
                    }
                val recipes = try {
                    response.decodeList<Recipe>()
                } catch (e: Exception) {
                    Log.d("RecipeRepository", "MyRecipes Join Fallback Triggered: ${e.localizedMessage}")
                    client.from("recipes").select { filter { eq("recipe_author", authorId) } }.decodeList<Recipe>()
                }
                
                // 🌟 FIX: Flatten author info immediately so UI and Local DB can see it
                recipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }

                recipeDao?.insertRecipes(recipes.map { it.toEntity(json) })
                
                recipes
            } catch (e: Exception) {
                Log.w("RecipeRepository", "Notice: Empty or failed fetch for my recipes: ${e.localizedMessage}")
                recipeDao?.getMyRecipes(authorId)?.map { it.toDomain(json) } ?: emptyList()
            }
        }
    }

    suspend fun insertRecipe(recipe: Recipe): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanRecipe = recipe.copy(authorInfo = null)
            client.from("recipes").insert(cleanRecipe)
            recipeDao?.insertRecipes(listOf(recipe.toEntity(json)))
            Unit
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanRecipe = recipe.copy(authorInfo = null)
            client.from("recipes").update(cleanRecipe) {
                filter { eq("recipe_id", recipe.recipe_id ?: "") }
            }
            recipeDao?.insertRecipes(listOf(recipe.toEntity(json)))
            Unit
        }
    }

    suspend fun uploadRecipeImage(recipeId: String, imageBytes: ByteArray): Result<String> = uploadImage("recipe_$recipeId", imageBytes)

    suspend fun uploadImage(fileName: String, imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val okClient = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                .addFormDataPart(
                    "file",
                    "$fileName.jpg",
                    imageBytes.toRequestBody("image/*".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload")
                .post(requestBody)
                .build()

            okClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Cloudinary failed: ${response.message}")
                val responseBody = response.body?.string() ?: ""
                val jsonRes = JSONObject(responseBody)
                jsonRes.getString("secure_url")
            }
        }
    }

    suspend fun getAvailableIngredients(): Result<List<Ingredient>> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("ingredient_units")
                .select(Columns.raw("*, units(default_quantity)"))
                .decodeList<Ingredient>()
        }
    }

    suspend fun toggleBookmark(userId: String, recipeId: String, isBookmarked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val table = client.from("recipe_bookmarks")
            if (isBookmarked) {
                table.delete {
                    filter {
                        eq("user_id", userId)
                        eq("recipe_id", recipeId)
                    }
                }
                recipeDao?.deleteBookmark(userId, recipeId)
            } else {
                table.insert(mapOf("user_id" to userId, "recipe_id" to recipeId))
                recipeDao?.insertBookmarks(listOf(com.example.foodieheal.Recipe.local.RecipeBookmarkEntity(userId, recipeId)))
            }
            Unit
        }
    }

    suspend fun getBookmarkedRecipes(userId: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val response = client.from("recipe_bookmarks")
                    .select(Columns.list("recipe_id")) { filter { eq("user_id", userId) } }
                val bookmarkedIds = response.decodeList<BookmarkId>().map { it.recipe_id }

                if (bookmarkedIds.isEmpty()) return@runCatching emptyList<Recipe>()

                val recipes = try {
                    client.from("recipes")
                        .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                            filter { isIn("recipe_id", bookmarkedIds) }
                        }.decodeList<Recipe>()
                } catch (e: Exception) {
                    Log.e("RecipeRepository", "Bookmarks Join Error: ${e.localizedMessage}")
                    client.from("recipes").select { filter { isIn("recipe_id", bookmarkedIds) } }.decodeList<Recipe>()
                }
                
                // 🌟 FIX: Flatten author info immediately so UI and Local DB can see it
                recipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }

                recipeDao?.insertRecipes(recipes.map { it.toEntity(json) })
                
                recipes
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error fetching bookmarks", e)
                recipeDao?.getBookmarkedRecipes(userId)?.map { it.toDomain(json) } ?: emptyList()
            }
        }
    }

    suspend fun getUserBookmarkIds(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.from("recipe_bookmarks")
                .select(Columns.list("recipe_id")) { filter { eq("user_id", userId) } }
            response.decodeList<BookmarkId>().map { it.recipe_id }
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("recipes").delete { filter { eq("recipe_id", recipeId) } }
            Unit
        }
    }

    suspend fun getRecipeById(recipeId: String): Result<Recipe?> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val recipe = try {
                    client.from("recipes")
                        .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                            filter { eq("recipe_id", recipeId) }
                        }.decodeSingle<Recipe>()
                } catch (e: Exception) {
                    Log.e("RecipeRepository", "RecipeById Join Error: ${e.localizedMessage}")
                    client.from("recipes").select { filter { eq("recipe_id", recipeId) } }.decodeSingle<Recipe>()
                }
                
                // 🌟 FIX: Flatten author info immediately so UI and Local DB can see it
                recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl

                recipeDao?.insertRecipes(listOf(recipe.toEntity(json)))
                
                recipe
            } catch (e: Exception) {
                recipeDao?.getRecipeById(recipeId)?.toDomain(json)
            }
        }
    }

    suspend fun getRecipesByIds(recipeIds: List<String>): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            if (recipeIds.isEmpty()) return@runCatching emptyList<Recipe>()
            try {
                val recipes = try {
                    client.from("recipes")
                        .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                            filter { isIn("recipe_id", recipeIds) }
                        }.decodeList<Recipe>()
                } catch (e: Exception) {
                    Log.e("RecipeRepository", "RecipesByIds Join Error: ${e.localizedMessage}")
                    client.from("recipes").select { filter { isIn("recipe_id", recipeIds) } }.decodeList<Recipe>()
                }
                
                // 🌟 FIX: Flatten author info immediately so UI and Local DB can see it
                recipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }
                recipes
            } catch (e: Exception) {
                val local = recipeDao?.getAllRecipes() ?: emptyList()
                local.filter { entity -> recipeIds.contains(entity.recipe_id) }
                    .map { it.toDomain(json) }
            }
        }
    }

    suspend fun getUserByCustomId(customId: String): Result<User?> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("users")
                .select { filter { eq("custom_id", customId) } }
                .decodeSingleOrNull<User>()
        }
    }
}

@Serializable
data class BookmarkId(@SerialName("recipe_id") val recipe_id: String)

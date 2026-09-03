package com.example.foodieheal.Recipe.Repo

import android.content.Context
import android.util.Log
import coil.imageLoader
import coil.request.ImageRequest
import com.example.foodieheal.Cloudinary.CloudinaryConfig
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.local.toDomain
import com.example.foodieheal.Recipe.local.toEntity
import com.example.foodieheal.SupabaseClient.client
import com.example.foodieheal.MainActivity
import com.example.foodieheal.User.local.UserDatabase
import com.example.foodieheal.User.local.toPublicEntity
import com.example.foodieheal.User.local.toDomain
import com.example.foodieheal.Recipe.Model.UnitDetails
import com.example.foodieheal.Recipe.local.RecipeBookmarkEntity
import com.example.foodieheal.Recipe.local.RecipeDao
import com.example.foodieheal.ingredients.local.IngredientsDatabase
import com.example.foodieheal.ingredients.repo.IngredientsRepository
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
    private val recipeDao: RecipeDao? = null,
    private val ingredientsRepository: IngredientsRepository? = null
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun getIngredientsRepo(): IngredientsRepository? {
        return ingredientsRepository ?: MainActivity.appContext?.let { ctx ->
            IngredientsRepository(IngredientsDatabase.getInstance(ctx).ingredientsDao())
        }
    }

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

                recipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }

                recipeDao?.let { dao ->
                    dao.insertRecipes(recipes.map { it.toEntity(json) })
                }
                
                recipes
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error fetching all recipes", e)
                recipeDao?.getAllRecipes()?.map { it.toDomain(json).apply { isOffline = true } } ?: emptyList()
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
                
                // Flatten author info immediately so UI and Local DB can see it
                recipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }

                recipeDao?.insertRecipes(recipes.map { it.toEntity(json) })
                
                recipes
            } catch (e: Exception) {
                Log.w("RecipeRepository", "Notice: Empty or failed fetch for my recipes: ${e.localizedMessage}")
                recipeDao?.getMyRecipes(authorId)?.map { it.toDomain(json).apply { isOffline = true } } ?: emptyList()
            }
        }
    }

    suspend fun insertRecipe(recipe: Recipe): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanRecipe = recipe.copy(authorInfo = null)
            client.from("recipes").insert(cleanRecipe)
            recipeDao?.insertRecipes(listOf(recipe.toEntity(json)))

            // Also cache the author info for offline profile viewing
            cacheAuthorInfo(recipe)
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanRecipe = recipe.copy(authorInfo = null)
            client.from("recipes").update(cleanRecipe) {
                filter { eq("recipe_id", recipe.recipe_id ?: "") }
            }
            recipeDao?.insertRecipes(listOf(recipe.toEntity(json)))

            // Also cache the author info for offline profile viewing
            cacheAuthorInfo(recipe)
        }
    }

    private suspend fun cacheAuthorInfo(recipe: Recipe) {
        if (recipe.author_id != null && recipe.authorName != null) {
            MainActivity.appContext?.let { context ->
                val dao = UserDatabase.getDatabase(context).userDao()
                val user = User(
                    id = null,
                    customId = recipe.author_id,
                    name = recipe.authorName,
                    profilePicUrl = recipe.authorImageUrl
                )
                dao.insertPublicUser(user.toPublicEntity())
            }
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
            val repo = getIngredientsRepo()
            if (repo != null) {
                val ingredients = repo.getIngredients()
                val allUnits = repo.getUnits().associateBy { it.unitID }
                val allIngredientUnits = repo.getAllIngredientUnits()

                ingredients.flatMap { ing ->
                    val unitsForIng = allIngredientUnits.filter { it.ingredientID == ing.ingredientId }
                    if (unitsForIng.isEmpty()) {
                        listOf(
                            Ingredient(
                                id = ing.ingredientId,
                                name = ing.ingredientName,
                                description = ing.ingredientDesc,
                                kcal = 0.0,
                                defaultUnit = "pieces",
                                unitDetails = UnitDetails(defaultQuantity = 1.0),
                                altNames = ing.ingredientAltNames
                            )
                        )
                    } else {
                        unitsForIng.map { iu ->
                            val unit = allUnits[iu.unitID]
                            Ingredient(
                                id = iu.ingredientUnitId,
                                name = ing.ingredientName,
                                description = ing.ingredientDesc,
                                kcal = iu.caloriesPerDefaultQuantity,
                                defaultUnit = unit?.unitDisplay?.ifEmpty { unit.unitName } ?: "pieces",
                                unitDetails = UnitDetails(defaultQuantity = unit?.defaultQuantity ?: 1.0),
                                altNames = ing.ingredientAltNames
                            )
                        }
                    }
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun toggleBookmark(userId: String, recipeId: String, isBookmarked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (userId.isBlank() || recipeId.isBlank()) return@runCatching

            // 1. Update Room first (Instant UI)
            if (isBookmarked) {
                recipeDao?.deleteBookmark(userId, recipeId)
            } else {
                recipeDao?.insertBookmarks(listOf(RecipeBookmarkEntity(userId, recipeId)))
            }

            // 2. Try Supabase
            try {
                val table = client.from("recipe_bookmarks")
                if (isBookmarked) {
                    table.delete {
                        filter {
                            eq("user_id", userId)
                            eq("recipe_id", recipeId)
                        }
                    }
                } else {
                    table.upsert(mapOf("user_id" to userId, "recipe_id" to recipeId))
                }
            } catch (e: Exception) {
                // If network failure or sync conflict, local Room is already updated successfully.
                Log.w("RecipeRepository", "Supabase bookmark sync notice: ${e.message}")
            }
            Unit
        }
    }


    suspend fun syncBookmarks(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (userId.isBlank()) return@runCatching

            // 1. Get server state
            val response = client.from("recipe_bookmarks")
                .select(Columns.list("recipe_id")) { filter { eq("user_id", userId) } }
            val serverIds = response.decodeList<BookmarkId>().map { it.recipeId }.toSet()

            // 2. Update local Room to match server exactly
            recipeDao?.clearBookmarks(userId)
            recipeDao?.insertBookmarks(serverIds.map { RecipeBookmarkEntity(userId, it) })

            Log.d("RecipeRepository", "Bookmark sync complete for $userId. Fetched ${serverIds.size} bookmarks.")
            Unit
        }
    }

    suspend fun getBookmarkedRecipes(userId: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            if (userId.isBlank()) return@runCatching emptyList()
            try {
                val response = client.from("recipe_bookmarks")
                    .select(Columns.list("recipe_id")) { filter { eq("user_id", userId) } }
                val bookmarkedIds = response.decodeList<BookmarkId>().map { it.recipeId }

                if (bookmarkedIds.isEmpty()) {
                    recipeDao?.clearBookmarks(userId)
                    return@runCatching emptyList()
                }

                val recipes = try {
                    client.from("recipes")
                        .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                            filter { isIn("recipe_id", bookmarkedIds) }
                        }.decodeList<Recipe>()
                } catch (e: Exception) {
                    Log.e("RecipeRepository", "Bookmarks Join Error: ${e.localizedMessage}")
                    client.from("recipes").select { filter { isIn("recipe_id", bookmarkedIds) } }.decodeList<Recipe>()
                }

                recipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }

                recipeDao?.insertRecipes(recipes.map { it.toEntity(json) })
                recipeDao?.clearBookmarks(userId)
                recipeDao?.insertBookmarks(bookmarkedIds.map { RecipeBookmarkEntity(userId, it) })

                recipes
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error fetching bookmarks", e)
                recipeDao?.getBookmarkedRecipes(userId)?.map { it.toDomain(json).apply { isOffline = true } } ?: emptyList()
            }
        }
    }

    suspend fun getLocalBookmarkedRecipes(userId: String): List<Recipe> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptyList()
        recipeDao?.getBookmarkedRecipes(userId)?.map { it.toDomain(json) } ?: emptyList()
    }

    suspend fun getUserBookmarkIds(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (userId.isBlank()) return@runCatching emptyList()
            val response = client.from("recipe_bookmarks")
                .select(Columns.list("recipe_id")) { filter { eq("user_id", userId) } }
            val ids = response.decodeList<BookmarkId>().map { it.recipeId }
            recipeDao?.clearBookmarks(userId)
            recipeDao?.insertBookmarks(ids.map { RecipeBookmarkEntity(userId, it) })
            ids
        }.recoverCatching { e ->
            Log.w("RecipeRepository", "Server fetch failed for bookmark IDs, falling back to local: ${e.message}")
            if (userId.isNotBlank()) {
                recipeDao?.getBookmarkIds(userId) ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("recipes").delete { filter { eq("recipe_id", recipeId) } }
            recipeDao?.deleteRecipe(recipeId)
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
                } catch (_: Exception) {
                    client.from("recipes").select { filter { eq("recipe_id", recipeId) } }.decodeSingle<Recipe>()
                }

                recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl

                recipeDao?.insertRecipes(listOf(recipe.toEntity(json)))
                
                recipe
            } catch (_: Exception) {
                recipeDao?.getRecipeById(recipeId)?.toDomain(json)?.apply { isOffline = true }
            }
        }
    }


     //EXCLUSIVE FUNCTION: Prioritizes local cache for instant meal planner viewing
    suspend fun getRecipeByIdLocalFirst(recipeId: String): Result<Recipe?> = withContext(Dispatchers.IO) {
        runCatching {
            val local = recipeDao?.getRecipeById(recipeId)?.toDomain(json)
            if (local != null) return@runCatching local.apply { isOffline = true }

            // If not in local, then try network
            getRecipeById(recipeId).getOrNull()
        }
    }

    suspend fun getRecipesByIds(recipeIds: List<String>): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            if (recipeIds.isEmpty()) return@runCatching emptyList()
            try {
                val recipes = try {
                    client.from("recipes")
                        .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                            filter { isIn("recipe_id", recipeIds) }
                        }.decodeList<Recipe>()
                } catch (_: Exception) {
                    client.from("recipes").select { filter { isIn("recipe_id", recipeIds) } }.decodeList<Recipe>()
                }

                recipes.forEach { recipe ->
                    recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                    recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
                }


                recipeDao?.insertRecipes(recipes.map { it.toEntity(json) })

                recipes
            } catch (_: Exception) {
                recipeDao?.getRecipesByIds(recipeIds)?.map { it.toDomain(json).apply { isOffline = true } } ?: emptyList()
            }
        }
    }

    suspend fun getFollowingRecipes(followedUserIds: List<String>): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            if (followedUserIds.isEmpty()) return@runCatching emptyList()

            val recipes = try {
                client.from("recipes")
                    .select(Columns.raw("*, users!recipe_author(name, profile_pic_url)")) {
                        filter {
                            isIn("recipe_author", followedUserIds)
                            // If visibility column is missing, the query might fail.
                            // We attempt to filter, but if it fails, we fetch without filtering and filter in memory.
                            try {
                                or {
                                    eq("visibility", "public")
                                    eq("visibility", "followers")
                                }
                            } catch (e: Exception) {
                                Log.w("RecipeRepository", "Visibility filter failed, table might need update: ${e.message}")
                            }
                        }
                    }.decodeList<Recipe>()
            } catch (e: Exception) {
                Log.e("RecipeRepository", "FollowingRecipes Join Error: ${e.localizedMessage}")
                val fallback = client.from("recipes").select {
                    filter {
                        isIn("recipe_author", followedUserIds)
                    }
                }.decodeList<Recipe>()
                // Filter in memory if server filter failed
                fallback.filter { it.visibility == "public" || it.visibility == "followers" }
            }

            recipes.forEach { recipe ->
                recipe.authorName = recipe.authorInfo?.name ?: recipe.authorName
                recipe.authorImageUrl = recipe.authorInfo?.profile_pic_url ?: recipe.authorImageUrl
            }

            // Cache for offline
            recipeDao?.insertRecipes(recipes.map { it.toEntity(json) })

            recipes
        }
    }

    suspend fun getUserByCustomId(customId: String): Result<User?> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val user = client.from("users")
                    .select { filter { eq("custom_id", customId) } }
                    .decodeSingleOrNull<User>()

                // Cache for offline
                MainActivity.appContext?.let { context ->
                    val dao = UserDatabase.getDatabase(context).userDao()
                    user?.let { dao.insertPublicUser(it.toPublicEntity()) }
                }
                user
            } catch (_: Exception) {
                // Offline fallback
                MainActivity.appContext?.let { context ->
                    val dao = UserDatabase.getDatabase(context).userDao()
                    dao.getPublicUser(customId)?.toDomain()
                }
            }
        }
    }

    suspend fun getUsersByCustomIds(customIds: List<String>): Result<List<User>> = withContext(Dispatchers.IO) {
        runCatching {
            if (customIds.isEmpty()) return@runCatching emptyList()
            try {
                val users = client.from("users")
                    .select { filter { isIn("custom_id", customIds) } }
                    .decodeList<User>()

                // Cache for offline
                MainActivity.appContext?.let { context ->
                    val dao = UserDatabase.getDatabase(context).userDao()
                    users.forEach { dao.insertPublicUser(it.toPublicEntity()) }
                }
                users
            } catch (_: Exception) {
                // Offline fallback
                MainActivity.appContext?.let { context ->
                    val dao = UserDatabase.getDatabase(context).userDao()
                    customIds.mapNotNull { dao.getPublicUser(it)?.toDomain() }
                } ?: emptyList()
            }
        }
    }


     //Pre-fetches images for a list of recipes and stores them in Coil's disk cache.
    suspend fun prefetchRecipeImages(recipes: List<Recipe>, context: Context) = withContext(Dispatchers.IO) {
        val imageLoader = context.imageLoader
        recipes.forEach { recipe ->
            recipe.recipeImageUrl?.let { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                imageLoader.enqueue(request)
            }
            recipe.authorImageUrl?.let { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }
}

@Serializable
data class BookmarkId(@SerialName("recipe_id") val recipeId: String)

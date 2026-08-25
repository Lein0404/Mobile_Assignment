package com.example.foodieheal.Recipe.viewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.MainActivity
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.Recipe.local.IngredientEntity
import com.example.foodieheal.Recipe.local.RecipeBookmarkEntity
import com.example.foodieheal.Recipe.local.RecipeDao
import com.example.foodieheal.Recipe.local.RecipeDatabase
import com.example.foodieheal.Recipe.local.RecipeEntity
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.Recipe.Model.IngredientItem
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.Model.UnitDetails
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.collections.plus

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {

    private fun getDao(): RecipeDao? {
        return MainActivity.appContext?.let { RecipeDatabase.getDatabase(it).recipeDao() }
    }

    private val json = Json { ignoreUnknownKeys = true }

    // 🌟 Shared Tab State (0: Popular, 1: My Recipes, 2: Bookmarks)
    var activeTab by mutableIntStateOf(0)

    var recipeList by mutableStateOf<List<Recipe>>(emptyList())
        private set

    var myRecipes by mutableStateOf<List<Recipe>>(emptyList())
        private set

    var bookmarkedRecipes by mutableStateOf<List<Recipe>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var availableIngredients by mutableStateOf<List<Ingredient>>(emptyList())
        private set

    var bookmarkedRecipeIds by mutableStateOf<Set<String>>(emptySet())
        private set

    private val _addRecipeSuccess = MutableSharedFlow<Boolean>()
    val addRecipeSuccess = _addRecipeSuccess.asSharedFlow()

    private val _updateRecipeSuccess = MutableSharedFlow<Boolean>()
    val updateRecipeSuccess = _updateRecipeSuccess.asSharedFlow()

    // 🌟 Shared Flow for bookmark feedback messages
    private val _bookmarkMessage = MutableSharedFlow<String>()
    val bookmarkMessage = _bookmarkMessage.asSharedFlow()

    private var isFetchingIngredients = false
    private var isFetchingAll = false

    var selectedRecipe by mutableStateOf<Recipe?>(null)
        private set

    var recipeAuthor by mutableStateOf<User?>(null)
        private set

    init {
        viewModelScope.launch {
            loadDataFromRoom()
            fetchAllRecipes(force = true)
            fetchAvailableIngredients()
        }
    }

    private suspend fun loadDataFromRoom() {
        val dao = getDao() ?: return
        try {
            val allEntities = dao.getAllRecipes()
            if (allEntities.isNotEmpty()) {
                recipeList = allEntities.map { mapEntityToRecipe(it) }
            }

            val ingredientEntities = dao.getAllIngredients()
            if (ingredientEntities.isNotEmpty()) {
                availableIngredients = ingredientEntities.map {
                    Ingredient(
                        id = it.id,
                        name = it.name,
                        kcal = it.kcal,
                        defaultUnit = it.defaultUnit,
                        unitDetails = UnitDetails(defaultQuantity = it.defaultQuantity ?: 1.0)
                    )
                }
            }

            val user = repository.getCurrentUserId()
            if (user != null) {
                val localIds = dao.getBookmarkIds(user)
                if (localIds.isNotEmpty()) bookmarkedRecipeIds = localIds.toSet()
            }
        } catch (e: Exception) {
            Log.e("RecipeViewModel", "Room load failed", e)
        }
    }

    private fun mapEntityToRecipe(entity: RecipeEntity): Recipe {
        return Recipe(
            recipe_id = entity.recipe_id,
            author_id = entity.author_id,
            recipeName = entity.recipeName,
            recipeDescription = entity.recipeDescription,
            recipeCourse = entity.recipeCourse,
            time = entity.time,
            calories = entity.calories,
            cookingSkill = entity.cookingSkill,
            estimatedBudget = entity.estimatedBudget,
            recipeStep = entity.recipeStep,
            recipeImageUrl = entity.recipeImageUrl,
            ingredients = try {
                json.decodeFromString<List<IngredientItem>>(entity.ingredientsJson)
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    fun fetchRecipeById(recipeId: String) {
        val cachedRecipe = recipeList.find { it.recipe_id == recipeId }
        if (cachedRecipe != null) {
            selectedRecipe = cachedRecipe
            cachedRecipe.author_id?.let { fetchAuthorData(it) }
            return
        }

        viewModelScope.launch {
            isLoading = true
            val dao = getDao()
            try {
                val localEntity = dao?.getRecipeById(recipeId)
                if (localEntity != null) {
                    val recipe = mapEntityToRecipe(localEntity)
                    selectedRecipe = recipe
                    recipe.author_id?.let { fetchAuthorData(it) }
                } else {
                    repository.getRecipeById(recipeId)
                        .onSuccess { recipe ->
                            selectedRecipe = recipe
                            recipe.author_id?.let { fetchAuthorData(it) }
                        }
                        .onFailure { e -> errorMessage = "Recipe not found: ${e.message}" }
                }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchAuthorData(authorId: String) {
        viewModelScope.launch {
            try {
                val author = repository.getUserByCustomId(authorId)
                    .getOrNull()
                recipeAuthor = author
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Failed to fetch author", e)
            }
        }
    }

    fun fetchAllRecipes(force: Boolean = false) {
        if (isFetchingAll) return
        viewModelScope.launch {
            val dao = getDao() ?: return@launch
            try {
                isFetchingAll = true
                if (!force && recipeList.isNotEmpty()) {
                    isFetchingAll = false
                    return@launch
                }
                if (recipeList.isEmpty()) isLoading = true
                repository.getAllRecipes()
                    .onSuccess { recipes ->
                        recipeList = recipes.sortedBy { it.recipe_id }
                        val entities = recipes.map { r ->
                            RecipeEntity(
                                recipe_id = r.recipe_id ?: "",
                                author_id = r.author_id,
                                recipeName = r.recipeName,
                                recipeDescription = r.recipeDescription,
                                recipeCourse = r.recipeCourse,
                                time = r.time,
                                calories = r.calories,
                                cookingSkill = r.cookingSkill,
                                estimatedBudget = r.estimatedBudget,
                                recipeStep = r.recipeStep,
                                recipeImageUrl = r.recipeImageUrl,
                                ingredientsJson = json.encodeToString(r.ingredients),
                                lastUpdated = r.lastUpdated
                            )
                        }
                        dao.clearRecipes()
                        dao.insertRecipes(entities)
                    }
            } catch (e: Exception) { } finally {
                isLoading = false
                isFetchingAll = false
            }
        }
    }

    fun fetchAvailableIngredients() {
        if (isFetchingIngredients) return
        viewModelScope.launch {
            val dao = getDao() ?: return@launch
            try {
                isFetchingIngredients = true
                repository.getAvailableIngredients()
                    .onSuccess { ingredients ->
                        availableIngredients = ingredients
                        val entities = ingredients.map {
                            IngredientEntity(
                                id = it.id ?: "",
                                name = it.name,
                                kcal = it.kcal,
                                defaultUnit = it.defaultUnit,
                                defaultQuantity = it.defaultQuantity // 🌟 Map new field
                            )
                        }
                        dao.clearIngredients()
                        dao.insertIngredients(entities)
                    }
            } catch (e: Exception) { } finally {
                isFetchingIngredients = false
            }
        }
    }

    fun fetchBookmarkIds(userId: String, force: Boolean = false) {
        // 🌟 Ensure we use the short ID (U001) for both Room and Supabase
        viewModelScope.launch {
            val dao = getDao() ?: return@launch

            // 🌟 1. If memory already has IDs and we aren't forcing, don't fetch from server
            // This prevents "Reverting Icon" when switching tabs quickly
            if (!force && bookmarkedRecipeIds.isNotEmpty()) {
                return@launch
            }

            // 2. Load from Room as fallback
            if (bookmarkedRecipeIds.isEmpty()) {
                val localIds = dao.getBookmarkIds(userId)
                if (localIds.isNotEmpty()) bookmarkedRecipeIds = localIds.toSet()
            }

            // 3. Sync with Supabase in background
            repository.getUserBookmarkIds(userId)
                .onSuccess { ids ->
                    bookmarkedRecipeIds = ids.toSet()
                    dao.clearBookmarks(userId)
                    dao.insertBookmarks(ids.map { RecipeBookmarkEntity(userId, it) })
                }
        }
    }

    fun toggleBookmark(userId: String, recipeId: String, recipeName: String) {
        val isBookmarked = bookmarkedRecipeIds.contains(recipeId)
        viewModelScope.launch {
            val dao = getDao()

            // 🌟 1. Update Memory IDs IMMEDIATELY
            bookmarkedRecipeIds = if (isBookmarked) bookmarkedRecipeIds - recipeId else bookmarkedRecipeIds + recipeId

            // 🌟 2. Update Memory List IMMEDIATELY (so Tab 2 updates without flickering)
            if (isBookmarked) {
                bookmarkedRecipes = bookmarkedRecipes.filter { it.recipe_id != recipeId }
            } else {
                // Find the recipe object from the main list to add it to bookmarks tab
                recipeList.find { it.recipe_id == recipeId }?.let {
                    bookmarkedRecipes = (bookmarkedRecipes + it).sortedBy { r -> r.recipe_id }
                }
            }

            // 3. Update Local Room Database
            try {
                if (isBookmarked) {
                    dao?.deleteBookmark(userId, recipeId)
                } else {
                    dao?.insertBookmarks(listOf(RecipeBookmarkEntity(userId, recipeId)))
                }
            } catch (e: Exception) { }

            // 4. Feedback
            if (!isBookmarked) {
                _bookmarkMessage.emit("Added to favorite: $recipeName")
            } else {
                _bookmarkMessage.emit("Removed from favorites: $recipeName")
            }

            // 5. Persist to Supabase in background
            repository.toggleBookmark(userId, recipeId, isBookmarked)
                .onFailure { e ->
                    Log.e("RecipeViewModel", "Supabase bookmark failed: ${e.message}", e)
                    // Optional: Revert UI if needed, but keeping it simple for now
                }
        }
    }

    fun fetchBookmarkedRecipes(userId: String, force: Boolean = false) {
        viewModelScope.launch {
            val dao = getDao() ?: return@launch

            // 🌟 Restore simple loading: Always sync with Supabase
            val local = dao.getBookmarkedRecipes(userId)
            if (local.isNotEmpty()) bookmarkedRecipes = local.map { mapEntityToRecipe(it) }

            isLoading = true
            repository.getBookmarkedRecipes(userId)
                .onSuccess { recipes ->
                    bookmarkedRecipes = recipes.sortedBy { it.recipe_id }
                }
            isLoading = false
        }
    }

    fun fetchMyRecipes(authorId: String, force: Boolean = false) {
        viewModelScope.launch {
            val dao = getDao() ?: return@launch

            // 🌟 Restore simple loading: Always sync with Supabase
            val local = dao.getMyRecipes(authorId)
            if (local.isNotEmpty()) myRecipes = local.map { mapEntityToRecipe(it) }

            isLoading = true
            repository.getMyRecipes(authorId)
                .onSuccess { recipes -> myRecipes = recipes.sortedBy { it.recipe_id } }
            isLoading = false
        }
    }

    fun addRecipe(recipe: Recipe, imageBytes: ByteArray? = null) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // 🌟 1. Optimistic Update: Add to memory lists immediately for zero-lag
            recipeList = (recipeList + recipe).sortedBy { it.recipe_id }
            myRecipes = (myRecipes + recipe).sortedBy { it.recipe_id }

            try {
                var finalRecipe = recipe
                if (imageBytes != null && recipe.recipe_id != null) {
                    repository.uploadRecipeImage(recipe.recipe_id, imageBytes)
                        .onSuccess { url -> finalRecipe = recipe.copy(recipeImageUrl = url) }
                }

                repository.insertRecipe(finalRecipe)
                    .onSuccess {
                        // 🌟 Show success message
                        _bookmarkMessage.emit("Successfully added: ${finalRecipe.recipeName}")

                        _addRecipeSuccess.emit(true)
                        // 🌟 2. Update memory with the version that has the real Image URL
                        recipeList = recipeList.map { if (it.recipe_id == finalRecipe.recipe_id) finalRecipe else it }
                        myRecipes = myRecipes.map { if (it.recipe_id == finalRecipe.recipe_id) finalRecipe else it }

                        // 3. Save to Room for persistence
                        saveRecipeToRoom(finalRecipe)
                    }
                    .onFailure { e ->
                        // Revert optimistic update on failure
                        recipeList = recipeList.filter { it.recipe_id != recipe.recipe_id }
                        myRecipes = myRecipes.filter { it.recipe_id != recipe.recipe_id }

                        val msg = e.message ?: "Unknown Error"
                        errorMessage = if (msg.contains("recipe_author", ignoreCase = true)) {
                            "Database Error: Please check Supabase columns."
                        } else {
                            msg.split("\n").firstOrNull() ?: "Save Failed"
                        }
                    }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun saveRecipeToRoom(r: Recipe) {
        val dao = getDao() ?: return
        try {
            val entity = RecipeEntity(
                recipe_id = r.recipe_id ?: "",
                author_id = r.author_id,
                recipeName = r.recipeName,
                recipeDescription = r.recipeDescription,
                recipeCourse = r.recipeCourse,
                time = r.time,
                calories = r.calories,
                cookingSkill = r.cookingSkill,
                estimatedBudget = r.estimatedBudget,
                recipeStep = r.recipeStep,
                recipeImageUrl = r.recipeImageUrl,
                ingredientsJson = json.encodeToString(r.ingredients),
                lastUpdated = r.lastUpdated
            )
            dao.insertRecipes(listOf(entity))
        } catch (e: Exception) { }
    }

    fun deleteRecipe(recipeId: String, userId: String) {
        viewModelScope.launch {
            isLoading = true
            // 🌟 Get recipe name before deleting for the success message
            val recipeName = recipeList.find { it.recipe_id == recipeId }?.recipeName ?: "Recipe"

            repository.deleteRecipe(recipeId)
                .onSuccess {
                    // 🌟 1. Update memory lists IMMEDIATELY for zero-lag
                    recipeList = recipeList.filter { it.recipe_id != recipeId }
                    myRecipes = myRecipes.filter { it.recipe_id != recipeId }
                    bookmarkedRecipes = bookmarkedRecipes.filter { it.recipe_id != recipeId }

                    // 🌟 2. Show success message
                    _bookmarkMessage.emit("Successfully deleted: $recipeName")

                    // 3. Force refresh from server to stay 100% accurate
                    fetchMyRecipes(userId, force = true)
                    fetchAllRecipes(force = true)
                }
            isLoading = false
        }
    }

    fun updateRecipe(recipe: Recipe, imageBytes: ByteArray? = null) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                var finalRecipe = recipe
                if (imageBytes != null && recipe.recipe_id != null) {
                    repository.uploadRecipeImage(recipe.recipe_id, imageBytes)
                        .onSuccess { url -> finalRecipe = recipe.copy(recipeImageUrl = url) }
                }
                repository.updateRecipe(finalRecipe)
                    .onSuccess {
                        _updateRecipeSuccess.emit(true)
                        // 🌟 Show success message
                        _bookmarkMessage.emit("Successfully updated: ${finalRecipe.recipeName}")

                        fetchAllRecipes(force = true)
                        recipe.author_id?.let { fetchMyRecipes(it) }
                        // Update Room
                        saveRecipeToRoom(finalRecipe)
                    }
                    .onFailure { e ->
                        errorMessage = "Update Failed: ${e.message}"
                    }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun generateNextRecipeId(): String {
        val maxId = recipeList.mapNotNull { it.recipe_id?.removePrefix("R")?.toIntOrNull() }.maxOrNull() ?: 0
        return "R${(maxId + 1).toString().padStart(3, '0')}"
    }
}
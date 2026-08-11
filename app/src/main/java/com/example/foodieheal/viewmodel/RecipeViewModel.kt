package com.example.foodieheal.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.model.Recipe
import com.example.foodieheal.model.Ingredient
import com.example.foodieheal.model.IngredientItem
import com.example.foodieheal.MainActivity
import com.example.foodieheal.database.AppDatabase
import com.example.foodieheal.database.RecipeEntity
import com.example.foodieheal.database.IngredientEntity
import com.example.foodieheal.database.BookmarkEntity
import com.example.foodieheal.database.RecipeDao
import com.example.foodieheal.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.runtime.mutableIntStateOf
import kotlin.onFailure

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {
    
    private fun getDao(): RecipeDao? {
        return MainActivity.appContext?.let { AppDatabase.getDatabase(it).recipeDao() }
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

    private var isFetchingIngredients = false
    private var isFetchingAll = false

    var selectedRecipe by mutableStateOf<Recipe?>(null)
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
                    Ingredient(id = it.id, name = it.name, kcal = it.kcal, defaultUnit = it.defaultUnit)
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
            } catch (e: Exception) { emptyList() }
        )
    }

    fun fetchRecipeById(recipeId: String) {
        // 1. Quick Check: If it's already in our memory cache, load it instantly
        val cachedRecipe = recipeList.find { it.recipe_id == recipeId }
        if (cachedRecipe != null) {
            selectedRecipe = cachedRecipe
            return
        }

        // 2. Fallback: If not found in memory, query database/repository asynchronously
        viewModelScope.launch {
            isLoading = true
            val dao = getDao()

            try {
                val localEntity = dao?.getRecipeById(recipeId) // Assumes you have this in RecipeDao
                if (localEntity != null) {
                    selectedRecipe = mapEntityToRecipe(localEntity)
                } else {
                    // If missing locally, pull from the network repository
                    repository.getRecipeById(recipeId) // Assumes you have this in Repository
                        .onSuccess { recipe ->
                            selectedRecipe = recipe
                        }
                        .onFailure { e ->
                            errorMessage = "Recipe not found: ${e.message}"
                        }
                }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
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
                                ingredientsJson = json.encodeToString(r.ingredients)
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
                            IngredientEntity(id = it.id ?: "", name = it.name, kcal = it.kcal, defaultUnit = it.defaultUnit)
                        }
                        dao.clearIngredients()
                        dao.insertIngredients(entities)
                    }
            } catch (e: Exception) { } finally {
                isFetchingIngredients = false
            }
        }
    }

    fun fetchBookmarkIds(userId: String) {
        viewModelScope.launch {
            val dao = getDao() ?: return@launch
            val localIds = dao.getBookmarkIds(userId)
            if (localIds.isNotEmpty()) bookmarkedRecipeIds = localIds.toSet()

            repository.getUserBookmarkIds(userId)
                .onSuccess { ids -> 
                    bookmarkedRecipeIds = ids.toSet()
                    dao.clearBookmarks(userId)
                    dao.insertBookmarks(ids.map { BookmarkEntity(userId, it) })
                }
        }
    }

    fun toggleBookmark(userId: String, recipeId: String) {
        val isBookmarked = bookmarkedRecipeIds.contains(recipeId)
        viewModelScope.launch {
            bookmarkedRecipeIds = if (isBookmarked) bookmarkedRecipeIds - recipeId else bookmarkedRecipeIds + recipeId
            repository.toggleBookmark(userId, recipeId, isBookmarked)
        }
    }

    fun fetchBookmarkedRecipes(userId: String) {
        viewModelScope.launch {
            val dao = getDao() ?: return@launch
            val local = dao.getBookmarkedRecipes(userId)
            if (local.isNotEmpty()) bookmarkedRecipes = local.map { mapEntityToRecipe(it) }

            isLoading = true
            repository.getBookmarkedRecipes(userId)
                .onSuccess { recipes -> bookmarkedRecipes = recipes.sortedBy { it.recipe_id } }
            isLoading = false
        }
    }

    fun fetchMyRecipes(authorId: String) {
        viewModelScope.launch {
            val dao = getDao() ?: return@launch
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
            try {
                var finalRecipe = recipe
                if (imageBytes != null && recipe.recipe_id != null) {
                    repository.uploadRecipeImage(recipe.recipe_id, imageBytes)
                        .onSuccess { url -> finalRecipe = recipe.copy(recipeImageUrl = url) }
                }
                repository.insertRecipe(finalRecipe)
                    .onSuccess {
                        _addRecipeSuccess.emit(true)
                        fetchAllRecipes(force = true)
                    }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteRecipe(recipeId: String, userId: String) {
        viewModelScope.launch {
            isLoading = true
            repository.deleteRecipe(recipeId)
                .onSuccess { fetchMyRecipes(userId); fetchAllRecipes(force = true) }
            isLoading = false
        }
    }

    fun generateNextRecipeId(): String {
        val maxId = recipeList.mapNotNull { it.recipe_id?.removePrefix("R")?.toIntOrNull() }.maxOrNull() ?: 0
        return "R${(maxId + 1).toString().padStart(3, '0')}"
    }
}

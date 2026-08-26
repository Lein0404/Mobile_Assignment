package com.example.foodieheal.Recipe.viewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.Recipe.Model.Recipe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val repository: RecipeRepository,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    // 🌟 UI States
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
    var selectedRecipe by mutableStateOf<Recipe?>(null)
        private set
    var recipeAuthor by mutableStateOf<User?>(null)
        private set
    var isNetworkAvailable by mutableStateOf(true)
        private set

    // 🌟 Event Flows
    private val _addRecipeSuccess = MutableSharedFlow<Boolean>()
    val addRecipeSuccess = _addRecipeSuccess.asSharedFlow()
    private val _updateRecipeSuccess = MutableSharedFlow<Boolean>()
    val updateRecipeSuccess = _updateRecipeSuccess.asSharedFlow()
    private val _bookmarkMessage = MutableSharedFlow<String>()
    val bookmarkMessage = _bookmarkMessage.asSharedFlow()

    // 🌟 Fetching Guards (Prevent duplicate calls)
    private var isFetchingAll = false
    private var isFetchingMyRecipes = false
    private var isFetchingBookmarks = false
    private var isFetchingIngredients = false

    init {
        observeNetworkStatus()
        refreshAll()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor?.isConnected?.collect { connected ->
                isNetworkAvailable = connected
                if (connected) refreshAll()
            }
        }
    }

    /**
     * 🌟 Manually updates the name/pic of all recipes in memory to match the current user.
     * This ensures the Cards and Details update the exact same field instantly.
     */
    fun syncRecipeAuthorInfo(user: User) {
        val cid = user.customId ?: return
        val updater: (Recipe) -> Recipe = { r ->
            if (r.author_id == cid) {
                r.copy(
                    authorName = user.name,
                    authorImageUrl = user.profilePicUrl,
                    // 🌟 Update authorInfo too (This is what the Detail screen and Card now share)
                    authorInfo = (r.authorInfo ?: com.example.foodieheal.Recipe.Model.AuthorInfo()).copy(
                        name = user.name,
                        profile_pic_url = user.profilePicUrl
                    )
                )
            } else r
        }

        recipeList = recipeList.map(updater)
        myRecipes = myRecipes.map(updater)
        bookmarkedRecipes = bookmarkedRecipes.map(updater)
        if (selectedRecipe?.author_id == cid) {
            selectedRecipe = updater(selectedRecipe!!)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            fetchAllRecipes(force = true)
            fetchAvailableIngredients()
        }
    }

    fun fetchAllRecipes(force: Boolean = false) {
        if (isFetchingAll) return
        viewModelScope.launch {
            try {
                isFetchingAll = true
                if (recipeList.isEmpty()) isLoading = true
                
                repository.getAllRecipes()
                    .onSuccess { result ->
                        recipeList = result.sortedBy { it.recipe_id }
                    }
                    .onFailure { e ->
                        errorMessage = "Failed to load recipes: ${e.message}"
                    }
            } finally {
                isLoading = false
                isFetchingAll = false
            }
        }
    }

    fun fetchAvailableIngredients() {
        if (isFetchingIngredients) return
        viewModelScope.launch {
            isFetchingIngredients = true
            repository.getAvailableIngredients().onSuccess { ingredients ->
                availableIngredients = ingredients
            }
            isFetchingIngredients = false
        }
    }

    fun fetchMyRecipes(authorId: String, force: Boolean = false) {
        if (isFetchingMyRecipes) return
        
        // 🌟 SAFETY: Clear stale data if switching accounts
        val belongsToSomeoneElse = myRecipes.isNotEmpty() && myRecipes.any { it.author_id != authorId }
        if (belongsToSomeoneElse || force) {
            myRecipes = emptyList()
        }

        viewModelScope.launch {
            try {
                isFetchingMyRecipes = true
                if (myRecipes.isEmpty()) isLoading = true
                repository.getMyRecipes(authorId)
                    .onSuccess { result ->
                        myRecipes = result.sortedBy { it.recipe_id }
                    }
            } finally {
                isLoading = false
                isFetchingMyRecipes = false
            }
        }
    }

    fun fetchBookmarkedRecipes(userId: String, force: Boolean = false) {
        if (isFetchingBookmarks) return
        if (force) bookmarkedRecipes = emptyList()

        viewModelScope.launch {
            try {
                isFetchingBookmarks = true
                if (bookmarkedRecipes.isEmpty()) isLoading = true
                repository.getBookmarkedRecipes(userId)
                    .onSuccess { result ->
                        bookmarkedRecipes = result.sortedBy { it.recipe_id }
                        bookmarkedRecipeIds = bookmarkedRecipes.mapNotNull { it.recipe_id }.toSet()
                    }
            } finally {
                isLoading = false
                isFetchingBookmarks = false
            }
        }
    }

    fun fetchBookmarkIds(userId: String) {
        viewModelScope.launch {
            repository.getUserBookmarkIds(userId).onSuccess { ids ->
                bookmarkedRecipeIds = ids.toSet()
            }
        }
    }

    fun toggleBookmark(userId: String, recipeId: String, recipeName: String) {
        val isBookmarked = bookmarkedRecipeIds.contains(recipeId)
        viewModelScope.launch {
            // 🌟 Optimistic Update: Immediate UI feedback
            bookmarkedRecipeIds = if (isBookmarked) bookmarkedRecipeIds - recipeId else bookmarkedRecipeIds + recipeId
            
            repository.toggleBookmark(userId, recipeId, isBookmarked).onSuccess {
                _bookmarkMessage.emit(if (isBookmarked) "Removed '$recipeName' from favorites" else "Added to favorites: $recipeName")
                fetchBookmarkedRecipes(userId)
            }
        }
    }

    fun fetchRecipeById(recipeId: String) {
        viewModelScope.launch {
            // 🌟 1. Clear previous recipe instantly so the loader shows for the new one
            selectedRecipe = null
            recipeAuthor = null
            
            isLoading = true
            repository.getRecipeById(recipeId)
                .onSuccess { recipe ->
                    selectedRecipe = recipe
                    recipe?.author_id?.let { fetchAuthorData(it) }
                }
            isLoading = false
        }
    }

    /**
     * 🌟 Clears the selected recipe data. 
     * Useful when exiting the details screen to prevent "stale data" flicker next time.
     */
    fun clearSelectedRecipe() {
        selectedRecipe = null
        recipeAuthor = null
    }

    fun fetchAuthorData(authorId: String) {
        viewModelScope.launch {
            repository.getUserByCustomId(authorId).onSuccess { author ->
                recipeAuthor = author
                // Update selected recipe with author name/pic for offline persistence
                selectedRecipe?.let { current ->
                    if (current.author_id == authorId && author != null) {
                        selectedRecipe = current.copy(
                            authorName = author.name,
                            authorImageUrl = author.profilePicUrl
                        )
                    }
                }
            }
        }
    }

    fun addRecipe(recipe: Recipe, imageBytes: ByteArray? = null) {
        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _bookmarkMessage.emit("No internet connection. Cannot add recipe.")
                return@launch
            }

            // 🌟 1. Instant Memory Update (Optimistic): Shows the new recipe card immediately
            recipeList = (recipeList + recipe).sortedBy { it.recipe_id }
            myRecipes = (myRecipes + recipe).sortedBy { it.recipe_id }

            isLoading = true
            var finalRecipe = recipe
            
            try {
                if (imageBytes != null && recipe.recipe_id != null) {
                    val uploadResult = repository.uploadRecipeImage(recipe.recipe_id, imageBytes)
                    if (uploadResult.isSuccess) {
                        finalRecipe = recipe.copy(recipeImageUrl = uploadResult.getOrNull())
                        // Update memory again with the real image URL
                        recipeList = recipeList.map { if (it.recipe_id == finalRecipe.recipe_id) finalRecipe else it }
                        myRecipes = myRecipes.map { if (it.recipe_id == finalRecipe.recipe_id) finalRecipe else it }
                    }
                }

                repository.insertRecipe(finalRecipe)
                    .onSuccess {
                        _addRecipeSuccess.emit(true)
                        _bookmarkMessage.emit("Successfully added: ${finalRecipe.recipeName}")
                        refreshAll()
                    }
                    .onFailure { e ->
                        // 🌟 Revert memory update on failure
                        recipeList = recipeList.filter { it.recipe_id != recipe.recipe_id }
                        myRecipes = myRecipes.filter { it.recipe_id != recipe.recipe_id }
                        parseError(e.message ?: "Save Failed")
                    }
            } catch (e: Exception) {
                errorMessage = e.message
                // Revert memory update
                recipeList = recipeList.filter { it.recipe_id != recipe.recipe_id }
                myRecipes = myRecipes.filter { it.recipe_id != recipe.recipe_id }
            } finally {
                isLoading = false
            }
        }
    }

    fun updateRecipe(recipe: Recipe, imageBytes: ByteArray? = null) {
        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _bookmarkMessage.emit("No internet connection. Cannot update recipe.")
                return@launch
            }
            
            // 🌟 1. Instant Memory Update (Optimistic): Fixes the "delay"
            // We find the old recipe to preserve authorInfo, so the card name changes but pic/author stays
            val oldRecipe = recipeList.find { it.recipe_id == recipe.recipe_id }
            val updatedForMemory = recipe.copy(
                authorInfo = oldRecipe?.authorInfo,
                authorName = oldRecipe?.authorName,
                authorImageUrl = oldRecipe?.authorImageUrl
            )
            
            recipeList = recipeList.map { if (it.recipe_id == recipe.recipe_id) updatedForMemory else it }
            myRecipes = myRecipes.map { if (it.recipe_id == recipe.recipe_id) updatedForMemory else it }
            if (selectedRecipe?.recipe_id == recipe.recipe_id) {
                selectedRecipe = updatedForMemory
            }

            isLoading = true
            var finalRecipe = recipe
            
            try {
                if (imageBytes != null && recipe.recipe_id != null) {
                    val uploadResult = repository.uploadRecipeImage(recipe.recipe_id, imageBytes)
                    if (uploadResult.isSuccess) {
                        finalRecipe = recipe.copy(recipeImageUrl = uploadResult.getOrNull())
                        // Update memory again with the new image URL
                        val updatedWithImage = updatedForMemory.copy(recipeImageUrl = finalRecipe.recipeImageUrl)
                        recipeList = recipeList.map { if (it.recipe_id == recipe.recipe_id) updatedWithImage else it }
                        myRecipes = myRecipes.map { if (it.recipe_id == recipe.recipe_id) updatedWithImage else it }
                    }
                }

                repository.updateRecipe(finalRecipe)
                    .onSuccess {
                        _updateRecipeSuccess.emit(true)
                        _bookmarkMessage.emit("Successfully updated: ${finalRecipe.recipeName}")
                        
                        // 2. Background Refresh to sync with DB exactly
                        refreshAll()
                    }
                    .onFailure { e ->
                        errorMessage = "Update Failed: ${e.message}"
                        // 🌟 Revert memory update on failure
                        oldRecipe?.let { old ->
                            recipeList = recipeList.map { if (it.recipe_id == old.recipe_id) old else it }
                            myRecipes = myRecipes.map { if (it.recipe_id == old.recipe_id) old else it }
                        }
                    }
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteRecipe(recipeId: String, userId: String) {
        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _bookmarkMessage.emit("No internet connection. Cannot delete recipe.")
                return@launch
            }
            isLoading = true
            repository.deleteRecipe(recipeId).onSuccess {
                _bookmarkMessage.emit("Recipe deleted successfully.")
                refreshAll()
                fetchMyRecipes(userId, force = true)
            }
            isLoading = false
        }
    }

    private fun parseError(msg: String) {
        errorMessage = if (msg.contains("recipe_author", ignoreCase = true)) {
            "Database Error: Missing author link. Please check Supabase."
        } else {
            msg.split("\n").firstOrNull() ?: "Operation Failed"
        }
    }

    fun generateNextRecipeId(): String {
        val maxId = recipeList.mapNotNull { it.recipe_id?.removePrefix("R")?.toIntOrNull() }.maxOrNull() ?: 0
        return "R${(maxId + 1).toString().padStart(3, '0')}"
    }

    fun clearUserData() {
        myRecipes = emptyList()
        bookmarkedRecipes = emptyList()
        bookmarkedRecipeIds = emptySet()
        selectedRecipe = null
        recipeAuthor = null
    }
}

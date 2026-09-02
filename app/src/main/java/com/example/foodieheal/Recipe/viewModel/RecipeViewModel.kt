package com.example.foodieheal.Recipe.viewModel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.Recipe.Model.Recipe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class RecipeViewModel(
    application: Application,
    private val repository: RecipeRepository,
    private val networkMonitor: NetworkMonitor? = null
) : AndroidViewModel(application) {

    // UI States
    var activeTab by mutableIntStateOf(0)
    var recipeList by mutableStateOf<List<Recipe>>(emptyList())
        private set
    var followingRecipes by mutableStateOf<List<Recipe>>(emptyList())
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
    var followedUserIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var selectedRecipe by mutableStateOf<Recipe?>(null)
        private set
    var recipeAuthor by mutableStateOf<User?>(null)
        private set
    var isNotFound by mutableStateOf(false)
        private set
    var isNetworkAvailable by mutableStateOf(true)
        private set

    // Event Flows
    private val _addRecipeSuccess = MutableSharedFlow<Boolean>()
    val addRecipeSuccess = _addRecipeSuccess.asSharedFlow()
    private val _updateRecipeSuccess = MutableSharedFlow<Boolean>()
    val updateRecipeSuccess = _updateRecipeSuccess.asSharedFlow()
    private val _bookmarkMessage = MutableSharedFlow<String>()
    val bookmarkMessage = _bookmarkMessage.asSharedFlow()

    // Fetching Guards (Prevent duplicate calls)
    private var isFetchingAll = false
    private var isFetchingFollowing = false
    private var isFetchingMyRecipes = false
    private var isFetchingBookmarks = false
    private var isFetchingIngredients = false

    // Track active toggle jobs to allow cancellation/restarts
    private val bookmarkJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private var currentCustomId: String? = null

    init {
        observeNetworkStatus()
        refreshAll()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor?.isConnected?.collect { connected ->
                isNetworkAvailable = connected
                if (connected) {
                    refreshAll()
                    currentCustomId?.let { cid ->
                        fetchBookmarkIds(cid)
                        repository.syncBookmarks(cid)
                    }
                }
            }
        }
    }


     //Manually updates the name/pic of all recipes in memory to match the current user.
     //This ensures the Cards and Details update the exact same field instantly.
    fun syncRecipeAuthorInfo(user: User) {
        val cid = user.customId ?: return
        currentCustomId = cid
        val updater: (Recipe) -> Recipe = { r ->
            if (r.author_id == cid) {
                r.copy(
                    authorName = user.name,
                    authorImageUrl = user.profilePicUrl,
                    // Update authorInfo too (This is what the Detail screen and Card now share)
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
            // We'll fetch following recipes if a user is logged in
        }
    }

    fun fetchAllRecipes(force: Boolean = false) {
        if (isFetchingAll && !force) return
        viewModelScope.launch {
            try {
                isFetchingAll = true
                if (recipeList.isEmpty() || force) isLoading = true
                
                repository.getAllRecipes()
                    .onSuccess { result ->
                        recipeList = result.sortedBy { it.recipe_id }
                        // Recovery: If some names are missing (Join failed), fetch them by ID automatically
                        fetchMissingAuthorInfo(result)
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

     // Recovery logic: Just like the Detail Screen, if a recipe arrives
     // without a name, we fetch the User data by its ID separately.

    private fun fetchMissingAuthorInfo(recipes: List<Recipe>) {
        val missingIds = recipes.filter { it.authorName.isNullOrEmpty() && !it.author_id.isNullOrEmpty() }
            .mapNotNull { it.author_id }
            .distinct()

        if (missingIds.isEmpty()) return

        viewModelScope.launch {
            // BATCH FETCH: Get all missing authors in ONE request instead of a slow loop
            repository.getUsersByCustomIds(missingIds).onSuccess { users ->
                if (users.isNotEmpty()) {
                    val userMap = users.associateBy { it.customId }
                    val updater: (Recipe) -> Recipe = { r ->
                        userMap[r.author_id]?.let { u ->
                            r.copy(authorName = u.name, authorImageUrl = u.profilePicUrl)
                        } ?: r
                    }
                    recipeList = recipeList.map(updater)
                    myRecipes = myRecipes.map(updater)
                    bookmarkedRecipes = bookmarkedRecipes.map(updater)
                    followingRecipes = followingRecipes.map(updater)
                }
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

    fun fetchFollowingRecipes(followerId: String) {
        if (isFetchingFollowing) return
        viewModelScope.launch {
            try {
                isFetchingFollowing = true
                isLoading = true

                // 1. Get list of followed users from FollowRepository (or similar)
                // For now, let's assume we have a way to get followed IDs.
                // In a real app, you might want to inject FollowRepository here or pass the IDs.
                // Since I just created FollowRepository, I'll use it if I can,
                // but RecipeRepository doesn't know about it.

                // Let's use FollowRepository to get the IDs first.
                val followRepo = com.example.foodieheal.User.Repo.FollowRepository()
                val following = followRepo.getFollowing(followerId)
                val followedIds = following.filter { it.status == "ACCEPTED" }.mapNotNull { it.followingId }
                followedUserIds = followedIds.toSet()

                if (followedIds.isNotEmpty()) {
                    repository.getFollowingRecipes(followedIds).onSuccess { recipes ->
                        followingRecipes = recipes.sortedByDescending { it.lastUpdated ?: "" }
                        fetchMissingAuthorInfo(recipes)
                    }
                } else {
                    followingRecipes = emptyList()
                }
            } finally {
                isLoading = false
                isFetchingFollowing = false
            }
        }
    }

    fun fetchMyRecipes(authorId: String, force: Boolean = false) {
        if (isFetchingMyRecipes) return

        // Clear stale data if switching accounts
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
                        fetchMissingAuthorInfo(result)
                    }
            } finally {
                isLoading = false
                isFetchingMyRecipes = false
            }
        }
    }

    fun fetchBookmarkedRecipes(userId: String, force: Boolean = false) {
        if (userId.isBlank()) return
        currentCustomId = userId
        if (isFetchingBookmarks) return
        if (force) bookmarkedRecipes = emptyList()

        viewModelScope.launch {
            try {
                isFetchingBookmarks = true
                // Local-first: immediately load cached bookmarks from Room so UI is populated with zero delay
                val local = repository.getLocalBookmarkedRecipes(userId)
                if (local.isNotEmpty()) {
                    val currentIds = bookmarkedRecipes.mapNotNull { it.recipe_id }.toSet()
                    val merged = (local + bookmarkedRecipes).distinctBy { it.recipe_id }.sortedBy { it.recipe_id }
                    bookmarkedRecipes = merged
                    bookmarkedRecipeIds = bookmarkedRecipes.mapNotNull { it.recipe_id }.toSet()
                } else if (bookmarkedRecipes.isEmpty()) {
                    isLoading = true
                }

                repository.getBookmarkedRecipes(userId)
                    .onSuccess { result ->
                        bookmarkedRecipes = result.sortedBy { it.recipe_id }
                        bookmarkedRecipeIds = bookmarkedRecipes.mapNotNull { it.recipe_id }.toSet()
                        fetchMissingAuthorInfo(result)
                    }
            } finally {
                isLoading = false
                isFetchingBookmarks = false
            }
        }
    }

    fun fetchBookmarkIds(userId: String) {
        if (userId.isBlank()) return
        currentCustomId = userId
        viewModelScope.launch {
            repository.getUserBookmarkIds(userId).onSuccess { ids ->
                bookmarkedRecipeIds = ids.toSet()
            }
        }
    }

    fun toggleBookmark(userId: String, recipeId: String, recipeName: String) {
        if (userId.isBlank() || recipeId.isBlank()) return
        currentCustomId = userId

        if (!isNetworkAvailable) {
            viewModelScope.launch {
                _bookmarkMessage.emit(getApplication<Application>().getString(R.string.msg_wifi_required_bookmark))
            }
            return
        }

        // 1. Cancel any existing job for this specific recipe to prevent race conditions
        bookmarkJobs[recipeId]?.cancel()

        val isBookmarked = bookmarkedRecipeIds.contains(recipeId)

        // 2. Instant UI Update (Always happens regardless of network speed)
        bookmarkedRecipeIds = if (isBookmarked) bookmarkedRecipeIds - recipeId else bookmarkedRecipeIds + recipeId

        if (isBookmarked) {
            bookmarkedRecipes = bookmarkedRecipes.filter { it.recipe_id != recipeId }
        } else {
            val recipe = recipeList.find { it.recipe_id == recipeId }
                ?: myRecipes.find { it.recipe_id == recipeId }
                ?: followingRecipes.find { it.recipe_id == recipeId }
                ?: selectedRecipe?.takeIf { it.recipe_id == recipeId }

            if (recipe != null) {
                if (bookmarkedRecipes.none { it.recipe_id == recipeId }) {
                    bookmarkedRecipes = (bookmarkedRecipes + recipe).sortedBy { r -> r.recipe_id }
                }
            }
        }

        // 3. Launch the new request (This job can be cancelled by the next click)
        bookmarkJobs[recipeId] = viewModelScope.launch {
            try {
                repository.toggleBookmark(userId, recipeId, isBookmarked).onSuccess {
                    // Check if this job is still active before showing toast or updating state
                    // This prevents "Old" jobs from overriding the current UI if they finish late.
                    if (!isActive) return@launch

                    val message = if (isBookmarked) {
                        if (isNetworkAvailable) getApplication<Application>().getString(R.string.msg_removed_from_favorites, recipeName)
                        else getApplication<Application>().getString(R.string.msg_removed_locally, recipeName)
                    } else {
                        if (isNetworkAvailable) getApplication<Application>().getString(R.string.msg_added_to_favorites, recipeName)
                        else getApplication<Application>().getString(R.string.msg_bookmarked_locally, recipeName)
                    }
                    _bookmarkMessage.emit(message)
                }.onFailure { e ->
                    if (!isActive) return@launch
                    Log.e("RecipeViewModel", "Toggle bookmark error", e)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e("RecipeViewModel", "Bookmark exception", e)
                }
            } finally {
                // Cleanup job map
                bookmarkJobs.remove(recipeId)
            }
        }
    }

    fun fetchRecipeById(recipeId: String) {
        viewModelScope.launch {
            selectedRecipe = null
            recipeAuthor = null
            isNotFound = false
            isLoading = true
            repository.getRecipeById(recipeId)
                .onSuccess { recipe ->
                    selectedRecipe = recipe
                    if (recipe == null) isNotFound = true
                    else recipe.author_id?.let { fetchAuthorData(it) }
                }
                .onFailure { isNotFound = true }
            isLoading = false
        }
    }


     // Uses local-first logic for instant meal plan recipe viewing
    fun fetchRecipeLocalFirst(recipeId: String) {
        viewModelScope.launch {
            selectedRecipe = null
            recipeAuthor = null
            isNotFound = false
            isLoading = true

            repository.getRecipeByIdLocalFirst(recipeId)
                .onSuccess { recipe ->
                    selectedRecipe = recipe
                    if (recipe == null) isNotFound = true
                    else recipe.author_id?.let { fetchAuthorData(it) }
                }
                .onFailure { isNotFound = true }
            isLoading = false
        }
    }


     //Clears the selected recipe data.
     //Useful when exiting the details screen to prevent "stale data" flicker next time.

    fun clearSelectedRecipe() {
        selectedRecipe = null
        recipeAuthor = null
        isNotFound = false
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
                _bookmarkMessage.emit(getApplication<Application>().getString(R.string.msg_no_internet_add_recipe))
                return@launch
            }

            if (recipe.author_id.isNullOrBlank()) {
                errorMessage = getApplication<Application>().getString(R.string.error_missing_author_link)
                return@launch
            }

            // 1. Instant Memory Update (Optimistic): Shows the new recipe card immediately
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
                        _bookmarkMessage.emit(getApplication<Application>().getString(R.string.msg_recipe_added_success, finalRecipe.recipeName))
                        refreshAll()
                    }
                    .onFailure { e ->
                        // Revert memory update on failure
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
                _bookmarkMessage.emit(getApplication<Application>().getString(R.string.msg_no_internet_update_recipe))
                return@launch
            }

            // Instant Memory Update (Optimistic): Fixes the "delay"
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
                        _bookmarkMessage.emit(getApplication<Application>().getString(R.string.msg_recipe_updated_success, finalRecipe.recipeName))

                        // 2. Background Refresh to sync with DB exactly
                        refreshAll()
                    }
                    .onFailure { e ->
                        errorMessage = "Update Failed: ${e.message}"
                        // Revert memory update on failure
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
                _bookmarkMessage.emit(getApplication<Application>().getString(R.string.msg_no_internet_delete_recipe))
                return@launch
            }
            isLoading = true
            repository.deleteRecipe(recipeId).onSuccess {
                Log.d("RecipeViewModel", "Delete successful for rid: $recipeId. Filtering locally...")

                // Immediate Local Update: Use a copy to ensure state change triggers UI
                val updatedMyRecipes = myRecipes.filter { it.recipe_id != recipeId }
                val updatedRecipeList = recipeList.filter { it.recipe_id != recipeId }
                val updatedBookmarkedRecipes = bookmarkedRecipes.filter { it.recipe_id != recipeId }

                myRecipes = updatedMyRecipes
                recipeList = updatedRecipeList
                bookmarkedRecipes = updatedBookmarkedRecipes

                // Update bookmark IDs set
                if (bookmarkedRecipeIds.contains(recipeId)) {
                    bookmarkedRecipeIds = bookmarkedRecipeIds.toMutableSet().apply { remove(recipeId) }
                }

                _bookmarkMessage.emit(getApplication<Application>().getString(R.string.msg_recipe_deleted_success))

                // Delay the background refresh slightly to give Supabase DB time to propagate the deletion
                // and to prevent a race condition where the fetch returns the old data.
                viewModelScope.launch {
                    delay(1000)
                    refreshAll()
                    fetchMyRecipes(userId, force = false) // Don't force clear again, we already filtered
                }
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

    fun showOfflinePlannerMessage() {
        viewModelScope.launch {
            _bookmarkMessage.emit(getApplication<Application>().getString(R.string.msg_wifi_required_planner))
        }
    }

    fun generateNextRecipeId(): String {
        val maxId = recipeList.mapNotNull { it.recipe_id?.removePrefix("R")?.toIntOrNull() }.maxOrNull() ?: 0
        return "R${(maxId + 1).toString().padStart(3, '0')}"
    }

    fun clearUserData() {
        myRecipes = emptyList()
        followingRecipes = emptyList()
        followedUserIds = emptySet()
        bookmarkedRecipes = emptyList()
        bookmarkedRecipeIds = emptySet()
        selectedRecipe = null
        recipeAuthor = null
    }
}

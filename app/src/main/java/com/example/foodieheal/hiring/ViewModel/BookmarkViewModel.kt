package com.example.foodieheal.hiring.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.hiring.data.BookmarkRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.Chef.model.Chef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookmarkViewModel(
    val chefBookmarkRepo: BookmarkRepository = BookmarkRepository(),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    var isBookmarked by mutableStateOf(false)
        private set

    private val _bookmarkedChefsFlow = MutableStateFlow<List<Chef>>(emptyList())
    val bookmarkedChefsFlow: StateFlow<List<Chef>> = _bookmarkedChefsFlow.asStateFlow()

    var bookmarkedChefsList by mutableStateOf<List<Chef>>(emptyList())
        private set

    var bookmarkedChefIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var isLoadingBookmarks by mutableStateOf(false)
        private set

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var lastUserId: String? = null

    init {
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.isConnected.collect { connected ->
                    _isNetworkAvailable.value = connected
                    if (connected && !lastUserId.isNullOrBlank()) {
                        fetchBookmarkedChefs(lastUserId!!, forceRefresh = false)
                    }
                }
            }
        }
    }

    fun isChefBookmarked(chefId: String): Boolean {
        return bookmarkedChefIds.contains(chefId)
    }

    // Toggle bookmark (add or remove)
    fun onBookmarkToggled(context: Context, userId: String, chefId: String, chefName: String) {
        if (userId.isBlank() || chefId.isBlank()) return

        val wasBookmarked = isChefBookmarked(chefId)

        // Optimistic UI update
        bookmarkedChefIds = if (wasBookmarked) {
            bookmarkedChefIds - chefId
        } else {
            bookmarkedChefIds + chefId
        }

        viewModelScope.launch {
            try {
                if (wasBookmarked) {
                    chefBookmarkRepo.removeBookmark(userId, chefId)
                    Toast.makeText(context, "Removed $chefName from bookmarks", Toast.LENGTH_SHORT).show()
                } else {
                    chefBookmarkRepo.addBookmark(userId, chefId)
                    Toast.makeText(context, "Chef $chefName added to bookmarks", Toast.LENGTH_SHORT).show()
                }

                fetchBookmarkedChefs(userId, forceRefresh = false)
            } catch (e: Exception) {
                // Revert local state on network/DB failure
                bookmarkedChefIds = if (wasBookmarked) {
                    bookmarkedChefIds + chefId
                } else {
                    bookmarkedChefIds - chefId
                }

                Toast.makeText(context, "Failed to update bookmark", Toast.LENGTH_SHORT).show()
                Log.e("BookmarkViewModel", "Error toggling bookmark", e)
            }
        }
    }

    fun fetchBookmarkedChefs(userId: String, forceRefresh: Boolean = false) {
        if (userId.isBlank()) return
        lastUserId = userId

        viewModelScope.launch {
            if (bookmarkedChefsList.isEmpty() || forceRefresh) {
                isLoadingBookmarks = true
            }
            try {
                val chefs = chefBookmarkRepo.getBookmarkedChefs(userId)
                bookmarkedChefsList = chefs
                _bookmarkedChefsFlow.value = chefs
                bookmarkedChefIds = chefs.map { it.chefId.ifEmpty { it.id } }.toSet()
            } catch (e: Exception) {
                Log.e("BookmarkViewModel", "Error fetching bookmarked chefs", e)
            } finally {
                isLoadingBookmarks = false
            }
        }
    }
}
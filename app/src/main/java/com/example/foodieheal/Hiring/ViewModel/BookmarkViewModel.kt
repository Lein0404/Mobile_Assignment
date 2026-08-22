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
import com.example.mobileassignmentloginpart.Model.Chef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookmarkViewModel(
    val chefBookmarkRepo: BookmarkRepository = BookmarkRepository()
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

                fetchBookmarkedChefs(userId)
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

    fun fetchBookmarkedChefs(userId: String) {
        if (userId.isBlank()) return

        viewModelScope.launch {
            isLoadingBookmarks = true
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
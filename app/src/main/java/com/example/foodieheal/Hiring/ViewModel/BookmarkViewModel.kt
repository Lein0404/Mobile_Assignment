package com.example.foodieheal.Hiring.ViewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.mobileassignmentloginpart.Model.Chef
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

class BookmarkViewModel: ViewModel() {

    private val client = SupabaseClient.client

    val chefBookmarkRepo = ChefBookmark() // Renamed to avoid class name collision

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

    // Check status on screen load
    fun checkBookmarkStatus(userId: String, chefId: String) {
        if (userId.isBlank() || chefId.isBlank()) {
            Log.w("Bookmark", "checkBookmarkStatus skipped: userId or chefId is blank (userId='$userId', chefId='$chefId')")
            return
        }

        viewModelScope.launch {
            try {
                val result = client
                    .from("Chef_Bookmark")
                    .select {
                        filter {
                            eq("id", userId)
                            eq("chefId", chefId)
                        }
                    }
                    .decodeList<ChefBookmark>()

                isBookmarked = result.isNotEmpty()
            } catch (e: Exception) {
                Log.e("Bookmark", "Error checking bookmark status", e)
            }
        }
    }

    // Toggle bookmark (add or remove)
    fun onBookmarkToggled(userId: String, chefId: String) {
        if (userId.isBlank() || chefId.isBlank()) return

        val wasBookmarked = isChefBookmarked(chefId)

        bookmarkedChefIds = if (wasBookmarked) {
            bookmarkedChefIds - chefId
        } else {
            bookmarkedChefIds + chefId
        }

        viewModelScope.launch {
            try {
                if (wasBookmarked) {
                    // Delete bookmark from database
                    client.from("Chef_Bookmark")
                        .delete {
                            filter {
                                eq("id", userId)
                                eq("chefId", chefId)
                            }
                        }
                } else {
                    // Insert bookmark into database
                    val bookmarkData = mapOf(
                        "id" to userId,
                        "chefId" to chefId
                    )
                    client.from("Chef_Bookmark").insert(bookmarkData)
                }

                fetchBookmarkedChefs(userId)

            } catch (e: Exception) {
                bookmarkedChefIds = if (wasBookmarked) {
                    bookmarkedChefIds + chefId
                } else {
                    bookmarkedChefIds - chefId
                }
                Log.e("Bookmark", "Error toggling bookmark", e)
            }
        }
    }

    fun removeBookmark(userId: String, chefId: String) {
        if (userId.isBlank() || chefId.isBlank()) return

        // 1. Optimistically update local UI state immediately
        val previousChefIds = bookmarkedChefIds
        bookmarkedChefIds = bookmarkedChefIds - chefId

        // Also remove from local list if displayed in Bookmarks Tab
        bookmarkedChefsList = bookmarkedChefsList.filterNot {
            (it.chefId.ifEmpty { it.id }) == chefId
        }

        viewModelScope.launch {
            try {
                // 2. Remove from Supabase database
                chefBookmarkRepo.deleteBookmark(userId, chefId)

                // 3. Sync full list from server
                fetchBookmarkedChefs(userId)

            } catch (e: Exception) {
                // 4. Revert UI state on network failure
                bookmarkedChefIds = previousChefIds
                fetchBookmarkedChefs(userId) // Reload original state
                Log.e("Bookmark", "Error removing bookmark", e)
            }
        }
    }

    fun fetchBookmarkedChefs(userId: String) {
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                val chefs = chefBookmarkRepo.getBookmarkedChefs(userId)
                bookmarkedChefsList = chefs

                bookmarkedChefIds = chefs.map { it.chefId.ifEmpty { it.id } }.toSet()
            } catch (e: Exception) {
                Log.e("Bookmark", "Error fetching bookmarked chefs", e)
            }
        }
    }
}
package com.example.foodieheal.Hiring.ViewModel

import android.content.Context
import android.util.Log
import android.widget.Toast
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

    // Toggle bookmark (add or remove)
    fun onBookmarkToggled(context: Context, userId: String, chefId: String, chefName : String) {
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
                    // Delete bookmark from database
                    client.from("Chef_Bookmark")
                        .delete {
                            filter {
                                eq("id", userId)
                                eq("chefId", chefId)
                            }
                        }
                    Toast.makeText(context, "Removed $chefName from bookmarks", Toast.LENGTH_SHORT).show()
                } else {
                    // Insert bookmark into database
                    val bookmarkData = mapOf(
                        "id" to userId,
                        "chefId" to chefId
                    )
                    client.from("Chef_Bookmark").insert(bookmarkData)
                    Toast.makeText(context, "Chef $chefName add to bookmarked", Toast.LENGTH_SHORT).show()
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
                Log.e("Bookmark", "Error toggling bookmark", e)
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
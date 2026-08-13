package com.example.foodieheal.Hiring.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.model.Appointment
import io.github.jan.supabase.postgrest.from
import com.example.foodieheal.model.ChefBookmark
import com.example.mobileassignmentloginpart.Model.Chef
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

class ChefBookmark : ViewModel() {

    private val client = SupabaseClient.client

    suspend fun isChefBookmarked(userId: String, chefId: String): Boolean {
        if (userId.isBlank() || chefId.isBlank()) return false
        return try {
            val result = client.from("Chef_Bookmark")
                .select {
                    filter {
                        eq("id", userId)
                        eq("chefId", chefId)
                    }
                }.decodeList<ChefBookmark>()

            result.isNotEmpty()
        } catch (e: Exception) {
            Log.e("ChefBookmark", "Error checking bookmark status", e)
            false
        }
    }

    suspend fun addBookmark(userId: String, chefId: String, currentlyBookmarked: Boolean) {
        if (userId.isBlank() || chefId.isBlank()) return
        if (currentlyBookmarked) {
            // Delete record
            client.from("Chef_Bookmark").delete {
                filter {
                    eq("id", userId)
                    eq("chefId", chefId)
                }
            }
        } else {
            // Insert new record
            val bookmark = mapOf(
                "id" to userId,
                "chefId" to chefId
            )
            client.from("Chef_Bookmark").insert(bookmark)
        }
    }

    suspend fun getBookmarkedChefs(userId: String): List<Chef> {
        if (userId.isBlank()) return emptyList()

        return try {
            val bookmarks = client
                .from("Chef_Bookmark")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<ChefBookmark>()

            val chefIds = bookmarks.map { it.chefId }.filter { it.isNotBlank() }

            if (chefIds.isEmpty()) return emptyList()

            val chefs = client
                .from("Chef")
                .select {
                    filter {
                        isIn("chefId", chefIds)
                    }
                }
                .decodeList<Chef>()

            val ratedAppointments = client
                .from("Appointment")
                .select {
                    filter {
                        gt("rating", 0)
                    }
                }
                .decodeList<Appointment>()

            chefs.map { chef ->
                val idToMatch = chef.chefId.ifEmpty { chef.id }

                val chefRatings = ratedAppointments
                    .filter { it.chefId == idToMatch }
                    .mapNotNull { it.rating }

                val avgRating = if (chefRatings.isNotEmpty()) {
                    (chefRatings.average() * 10).toInt() / 10.0
                } else null

                chef.copy(averagerating = avgRating)
            }

        } catch (e: Exception) {
            Log.e("ChefBookmark", "Error fetching bookmarked chefs", e)
            emptyList()
        }
    }

    suspend fun deleteBookmark(userId: String, chefId: String) {
        if (userId.isBlank() || chefId.isBlank()) return

        client.from("Chef_Bookmark")
            .delete {
                filter {
                    eq("id", userId)
                    eq("chefId", chefId)
                }
            }
    }
}


@Serializable
data class ChefWrapper(
    val chef: Chef
)

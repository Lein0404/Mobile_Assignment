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

    suspend fun getBookmarkedChefs(userId: String): List<Chef> {
        if (userId.isBlank()) return emptyList()

        return try {
            // Fetch bookmark records for this user
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

            // Fetch matching chefs from 'Chef' table
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
}


@Serializable
data class ChefWrapper(
    val chef: Chef
)

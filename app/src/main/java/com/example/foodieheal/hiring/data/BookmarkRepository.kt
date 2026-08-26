package com.example.foodieheal.hiring.data

import android.util.Log
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.hiring.local.ChefBookmarkDao
import com.example.foodieheal.hiring.local.ChefDao
import com.example.foodieheal.hiring.local.toEntity
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.hiring.model.ChefBookmark
import com.example.foodieheal.Chef.model.Chef
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookmarkRepository(
    private var bookmarkDao: ChefBookmarkDao? = null,
    private var chefDao: ChefDao? = null
) {

    private fun getBookmarkDao(): ChefBookmarkDao? {
        if (bookmarkDao == null) {
            com.example.foodieheal.MainActivity.appContext?.let { ctx ->
                bookmarkDao = com.example.foodieheal.hiring.local.HiringDatabase.getInstance(ctx).chefBookmarkDao()
            }
        }
        return bookmarkDao
    }

    private fun getChefDao(): ChefDao? {
        if (chefDao == null) {
            com.example.foodieheal.MainActivity.appContext?.let { ctx ->
                chefDao = com.example.foodieheal.hiring.local.HiringDatabase.getInstance(ctx).chefDao()
            }
        }
        return chefDao
    }

    private val client = SupabaseClient.client

    suspend fun getBookmarkedChefs(userId: String): List<Chef> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptyList()
        val bDao = getBookmarkDao()
        val cDao = getChefDao()

        try {
            // Fetch bookmark records for this user
            val bookmarks = client
                .from("Chef_Bookmark")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<ChefBookmark>()

            // Cache bookmarks locally
            bDao?.let { dao ->
                dao.clearBookmarksForUser(userId)
                dao.insertBookmarks(bookmarks.map { it.toEntity() })
            }

            val chefIds = bookmarks.map { it.chefId }.filter { it.isNotBlank() }
            if (chefIds.isEmpty()) return@withContext emptyList()

            // Fetch matching chefs from 'Chef' table
            val chefs = client
                .from("Chef")
                .select {
                    filter {
                        isIn("chefId", chefIds)
                    }
                }
                .decodeList<Chef>()

            val ratedAppointments = try {
                client
                    .from("Appointment")
                    .select {
                        filter {
                            gt("rating", 0)
                        }
                    }
                    .decodeList<Appointment>()
            } catch (e: Exception) {
                Log.e("BookmarkRepository", "Could not fetch rated appointments", e)
                emptyList()
            }

            chefs.map { chef ->
                val idToMatch = chef.chefId.ifEmpty { chef.id }

                val chefRatings = ratedAppointments
                    .filter { appt ->
                        (appt.chefId.isNotBlank() && (appt.chefId == chef.chefId || appt.chefId == chef.id)) ||
                        (idToMatch.isNotBlank() && appt.chefId == idToMatch)
                    }
                    .mapNotNull { it.rating }

                val avgRating = if (chefRatings.isNotEmpty()) {
                    (chefRatings.average() * 10).toInt() / 10.0
                } else {
                    chef.averagerating
                }

                chef.copy(averagerating = avgRating)
            }
        } catch (e: Exception) {
            Log.e("BookmarkRepository", "Error fetching bookmarked chefs, falling back to local database", e)
            val cachedBookmarks = bDao?.getBookmarksForUser(userId) ?: emptyList()
            val cachedChefs = cachedBookmarks.mapNotNull { b ->
                cDao?.getChefById(b.chefId)?.toDomain()
            }
            cachedChefs
        }
    }

    suspend fun addBookmark(userId: String, chefId: String) = withContext(Dispatchers.IO) {
        val bookmarkData = mapOf(
            "id" to userId,
            "chefId" to chefId
        )
        client.from("Chef_Bookmark").insert(bookmarkData)
        getBookmarkDao()?.insertBookmark(
            ChefBookmark(
                id = "${userId}_${chefId}",
                userId = userId,
                chefId = chefId
            ).toEntity()
        )
    }

    suspend fun removeBookmark(userId: String, chefId: String) = withContext(Dispatchers.IO) {
        client.from("Chef_Bookmark")
            .delete {
                filter {
                    eq("id", userId)
                    eq("chefId", chefId)
                }
            }
        getBookmarkDao()?.deleteBookmark(userId, chefId)
    }
}

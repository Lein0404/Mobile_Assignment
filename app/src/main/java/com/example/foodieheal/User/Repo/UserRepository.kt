package com.example.foodieheal.User.Repo

import android.util.Log
import com.example.foodieheal.MainActivity
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.User.local.UserDatabase
import com.example.foodieheal.User.local.toPublicEntity
import com.example.foodieheal.User.local.toDomain
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    private fun getDao() = MainActivity.appContext?.let { UserDatabase.getDatabase(it).userDao() }

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val users = SupabaseClient.client.from("users").select().decodeList<User>()
            users.forEach { user ->
                getDao()?.insertPublicUser(user.toPublicEntity())
            }
            users
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
        try {
            val user = SupabaseClient.client.from("users").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<User>()
            
            user?.let { getDao()?.insertPublicUser(it.toPublicEntity()) }
            user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByCustomId(customId: String): User? = withContext(Dispatchers.IO) {
        try {
            val user = SupabaseClient.client.from("users").select {
                filter { eq("custom_id", customId) }
            }.decodeSingleOrNull<User>()
            
            user?.let { getDao()?.insertPublicUser(it.toPublicEntity()) }
            user
        } catch (e: Exception) {
            Log.w("UserRepository", "Offline mode: fetching user $customId from cache")
            getDao()?.getPublicUser(customId)?.toDomain()
        }
    }
}

package com.example.foodieheal.User.Repo

import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.User.Model.User
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("users").select().decodeList<User>()
    }

    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("users").select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<User>()
    }
}
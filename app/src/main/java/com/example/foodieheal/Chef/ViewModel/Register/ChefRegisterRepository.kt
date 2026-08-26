package com.example.foodieheal.Chef.ViewModel.Register

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.foodieheal.Cloudinary.uploadImageToCloudinary
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.User.Model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import java.util.UUID

class ChefRegisterRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {

    suspend fun isEmailRegistered(email: String): Boolean {
        val cleanEmail = email.trim()
        val chefExists = client.postgrest.from("Chef").select {
            filter { eq("email", cleanEmail) }
        }.decodeList<Chef>().isNotEmpty()
        if (chefExists) return true

        val userExists = client.postgrest.from("users").select {
            filter { eq("email", cleanEmail) }
        }.decodeList<User>().isNotEmpty()
        return userExists
    }

    suspend fun authenticateUser(email: String, password: String): String {
        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (signUpException: Exception) {
            Log.w(TAG, "signUpWith exception: ${signUpException.message}")
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (_: Exception) {
                throw signUpException
            }
        }

        var user = client.auth.currentUserOrNull()
        if (user == null) {
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                user = client.auth.currentUserOrNull()
            } catch (e: Exception) {
                Log.w(TAG, "signInWith fallback failed: ${e.message}")
            }
        }

        return user?.id ?: UUID.randomUUID().toString()
    }

    suspend fun uploadProfileImage(context: Context, uri: Uri): String {
        Log.d(TAG, "Start Cloudinary upload: $uri")
        val imageUrl = context.uploadImageToCloudinary(uri)
        Log.d(TAG, "Cloudinary URL: $imageUrl")
        return imageUrl
    }

    suspend fun insertChef(chef: Chef) {
        client.postgrest.from("Chef").insert(chef)
    }

    suspend fun getChefByUserId(userId: String): Chef? {
        return try {
            client.postgrest.from("Chef").select {
                filter { eq("chefId", userId) }
            }.decodeSingleOrNull<Chef>()
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching chef by userId: ${e.message}")
            null
        }
    }

    suspend fun updateChef(chef: Chef) {
        client.postgrest.from("Chef").update(chef) {
            filter {
                eq("chefId", chef.chefId)
            }
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    fun generateCustomChefId(): String = "C${(100..999).random()}"

    fun mapRegistrationError(message: String?): String {
        val msg = message ?: "Registration failed. Please try again."
        return when {
            msg.contains("row-level security", ignoreCase = true) ||
                    msg.contains("violates row-level security policy", ignoreCase = true) ->
                "Supabase RLS Error: Please add an INSERT policy or disable RLS on the 'Chef' table in Supabase Dashboard."

            msg.contains("already registered", ignoreCase = true) ||
                    msg.contains("already exists", ignoreCase = true) ->
                "This email is already registered. Please login instead."

            msg.contains("duplicate key", ignoreCase = true) ||
                    msg.contains("unique constraint", ignoreCase = true) ->
                "Chef account already created. Please login."

            else -> msg.lines().firstOrNull() ?: msg
        }
    }

    private companion object {
        const val TAG = "ChefRegister"
    }
}

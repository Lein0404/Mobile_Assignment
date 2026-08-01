package com.example.foodieheal.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.foodieheal.model.User
import com.example.foodieheal.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val client = SupabaseClient.client

    var currentUser by mutableStateOf<User?>(null)
        private set

    var currentChef by mutableStateOf<Chef?>(null)
        private set

    var isAdmin by mutableStateOf(false)
        private set

    var isChef by mutableStateOf(false)
        private set

    var loginSuccess by mutableStateOf(false)
        private set

    var registerSuccess by mutableStateOf(false)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    init {
        val user = client.auth.currentUserOrNull()
        if (user != null) {
            viewModelScope.launch {
                fetchUserData(user.id)
                loginSuccess = true
            }
        }
    }

    fun login(emailInput: String, passwordInput: String) {
        val cleanEmail = emailInput.trim()
        val cleanPassword = passwordInput.trim()

        if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
            errorMessage = "Email and password cannot be empty"
            return
        }

        // Hardcode for Admin Login
        if (cleanEmail == "admin@gmail.com" && cleanPassword == "admin1234") {
            isAdmin = true
            loginSuccess = true
            return
        }

        isProcessing = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                // 1. Authenticate with Supabase Auth
                client.auth.signInWith(Email) {
                    email = cleanEmail
                    password = cleanPassword
                }

                val currentUser = client.auth.currentUserOrNull()
                    ?: throw Exception("User session not found.")

                val userId = currentUser.id

                //Check current user got exists in 'Chef' supabase table or not
                val chefData = client.postgrest["Chef"]
                    .select { filter { eq("chefId", userId) } }
                    .decodeSingleOrNull<Chef>()

                if (chefData != null) {
                    // User is a Chef -> Check Approval Status
                    when (chefData.status?.lowercase()) {
                        "approved" -> {
                            currentChef = chefData
                            isChef = true
                            isAdmin = false
                            loginSuccess = true
                        }
                        "pending" -> {
                            client.auth.signOut()
                            errorMessage = "Your chef application is pending admin approval."
                        }
                        "rejected" -> {
                            client.auth.signOut()
                            errorMessage = "Your chef application has been rejected."
                        }
                        else -> {
                            client.auth.signOut()
                            errorMessage = "Account status unknown. Please contact support."
                        }
                    }
                    return@launch
                }

                // If not a Chef, check if user exists in "users" table
                // so no need use fetchUserData
                val userData = client.postgrest["users"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<User>()

                if (userData != null) {
                    isChef = false
                    isAdmin = false
                    loginSuccess = true
                } else {
                    // Not found in either table
                    client.auth.signOut()
                    errorMessage = "User not found."
                }

            } catch (e: Exception) {
                val rawMessage = e.message ?: "Login Failed"
                errorMessage = if (rawMessage.contains("Invalid login credentials", ignoreCase = true)) {
                    "Invalid email or password"
                } else {
                    rawMessage
                }
            } finally {
                isProcessing = false
            }
        }
    }

    fun register(emailInput: String, passwordInput: String) {
        isProcessing = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                client.auth.signUpWith(Email) {
                    email = emailInput
                    password = passwordInput
                }
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    setupNewUser(user.id, emailInput)
                } else {
                    errorMessage = "Register successful! Login now."
                    isProcessing = false
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Register Failed"
                isProcessing = false
            }
        }
    }

    private fun setupNewUser(uid: String, email: String) {
        viewModelScope.launch {
            try {
                // Generate U001, U002 etc using a simple timestamp for uniqueness
                val randomNum = (100..999).random()
                val customId = "U$randomNum"
                val newUser = User(id = uid, customId = customId, email = email, name = "User ($customId)")

                client.postgrest.from("users").insert(newUser)
                currentUser = newUser
                registerSuccess = true
            } catch (e: Exception) {
                errorMessage = "Database Error: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    private suspend fun fetchUserData(uid: String) {

        try {
            val user = client.postgrest.from("users").select {
                filter { eq("id", uid) }
            }.decodeSingleOrNull<User>()

            if (user != null) {
                currentUser = user
            } else {
                // If row doesn't exist, create a local placeholder with the user's email
                val authEmail = client.auth.currentUserOrNull()?.email ?: ""
                currentUser = User(id = uid, customId = "U001", email = authEmail, name = "New User")
            }
        } catch (e: Exception) {
            // If data is missing or NULL, fallback to a default user instead of showing ERROR
            val authEmail = client.auth.currentUserOrNull()?.email ?: ""
            currentUser = User(id = uid, customId = "U001", email = authEmail, name = "User")
        }
    }

    fun fetchChefData() {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                val chefData = client.postgrest["Chef"]
                    .select { filter { eq("chefId", userId) } }
                    .decodeSingleOrNull<Chef>()

                currentChef = chefData
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch chef profile"
            }
        }
    }

// Not using currently
    private suspend fun fetchChefData(uid: String) {
        try {
            val chef = client
                .postgrest
                .from("Chef")
                .select {
                    filter {
                        eq("chefId", uid)
                    }
                }
                .decodeSingleOrNull<Chef>()
            if (chef != null) {
                when (chef.status) {
                    "Approved" -> {
                        isChef = true
                    }
                    "Pending" -> {
                        errorMessage =
                            "Your chef account is waiting for admin approval."
                        client.auth.signOut()
                    }
                    "Rejected" -> {
                        errorMessage =
                            "Your chef registration has been rejected."
                        client.auth.signOut()
                    }
                }
            } else {
                errorMessage = "Account not found."
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to fetch chef data"
        }
    }

    fun updateProfile(name: String, email: String, profilePicUrl: String) {
        val uid = currentUser?.id ?: return
        isProcessing = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                val updatedUser = currentUser?.copy(name = name, email = email, profilePicUrl = profilePicUrl) ?: return@launch
                client.postgrest.from("users").update(updatedUser) {
                    filter { eq("id", uid) }
                }
                currentUser = updatedUser

                if (client.auth.currentUserOrNull()?.email != email) {
                    client.auth.updateUser { this.email = email }
                }

                errorMessage = "Profile Updated"
            } catch (e: Exception) {
                errorMessage = "Update Failed: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun updatePassword(newPassword: String) {
        if (newPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }
        viewModelScope.launch {
            try {
                client.auth.updateUser { password = newPassword }
                errorMessage = "Profile Updated"
            } catch (e: Exception) {
                errorMessage = e.message ?: "Password update failed"
            }
        }
    }

    fun forgotPassword(emailInput: String) {
        if (emailInput.isEmpty()) {
            errorMessage = "Please enter your email address"
            return
        }
        isProcessing = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                client.auth.resetPasswordForEmail(emailInput)
                errorMessage = "Reset link sent to your email"
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to send reset email"
                if (errorMessage.contains("rate limit", ignoreCase = true)) {
                    errorMessage = "Too many requests. Please try again in an hour."
                }
            } finally {
                isProcessing = false
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        currentUser = null
        loginSuccess = false
        registerSuccess = false
        errorMessage = ""
        viewModelScope.launch {
            try {
                client.auth.signOut()
            } catch (e: Exception) { }
            isProcessing = false
            onComplete()
        }
    }

    fun resetLoginState() {
        loginSuccess = false
        isAdmin = false
    }
}

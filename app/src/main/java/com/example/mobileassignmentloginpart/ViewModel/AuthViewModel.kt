package com.example.mobileassignmentloginpart.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileassignmentloginpart.Model.User
import com.example.mobileassignmentloginpart.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val client = SupabaseClient.client

    var currentUser by mutableStateOf<User?>(null)
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
        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            errorMessage = "Email and password cannot be empty"
            return
        }
        isProcessing = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                client.auth.signInWith(Email) {
                    email = emailInput
                    password = passwordInput
                }
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    fetchUserData(user.id)
                    loginSuccess = true
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Login Failed"
                if (errorMessage.contains("Invalid login credentials")) {
                    errorMessage = "Invalid email or password"
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
                errorMessage = "Password update failed"
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
                errorMessage = "Failed to send reset email"
            } finally {
                isProcessing = false
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        isProcessing = true
        viewModelScope.launch {
            try {
                client.auth.signOut()
            } catch (e: Exception) { }
            currentUser = null
            loginSuccess = false
            registerSuccess = false
            errorMessage = ""
            isProcessing = false
            onComplete()
        }
    }
}

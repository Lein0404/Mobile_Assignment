package com.example.foodieheal.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.foodieheal.model.User
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.MainActivity
import com.example.foodieheal.database.AppDatabase
import com.example.foodieheal.database.UserEntity
import com.example.foodieheal.database.ChefEntity
import com.example.foodieheal.database.UserDao
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val client = SupabaseClient.client
    
    private fun getDao(): UserDao? {
        return MainActivity.appContext?.let { AppDatabase.getDatabase(it).userDao() }
    }

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

    var isInitializing by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf("")
        private set

    var CheferrorMessage by mutableStateOf<String?>(null)
        private set

    var passwordUpdateSuccess by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            try {
                restoreOfflineSessionSync()
                
                if (currentUser != null || currentChef != null) {
                    loginSuccess = true
                }

                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    fetchUserData(user.id)
                    val chef = client.postgrest["Chef"].select { filter { eq("chefId", user.id) } }.decodeSingleOrNull<Chef>()
                    if (chef != null && chef.status.lowercase() == "approved") {
                        currentChef = chef
                        isChef = true
                        saveChefToCache(chef)
                    }
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Init error: ${e.message}")
            } finally {
                delay(500)
                isInitializing = false 
            }
        }
    }

    private suspend fun restoreOfflineSessionSync() {
        val dao = getDao() ?: return
        try {
            val localUser = dao.getUser()
            if (localUser != null) {
                currentUser = User(
                    id = localUser.id,
                    customId = localUser.customId,
                    email = localUser.email,
                    name = localUser.name,
                    profilePicUrl = localUser.profilePicUrl,
                    description = localUser.description,
                    weight = localUser.weight,
                    height = localUser.height,
                    age = localUser.age,
                    gender = localUser.gender,
                    bmi = localUser.bmi
                )
            }

            val localChef = dao.getChef()
            if (localChef != null) {
                currentChef = Chef(
                    id = localChef.id,
                    chefId = localChef.chefId,
                    name = localChef.name,
                    gender = localChef.gender,
                    age = localChef.age,
                    phoneNumber = localChef.phoneNumber,
                    email = localChef.email,
                    address = localChef.address,
                    state = localChef.state,
                    postcode = localChef.postcode,
                    experience = localChef.experience,
                    description = localChef.description,
                    status = localChef.status,
                    profilePictureUrl = localChef.profilePictureUrl,
                    averagerating = localChef.averagerating,
                    Pricing = localChef.Pricing
                )
                isChef = true
            }
        } catch (e: Exception) { }
    }

    private fun saveUserToCache(user: User) {
        viewModelScope.launch {
            val dao = getDao() ?: return@launch
            try {
                dao.insertUser(UserEntity(
                    id = user.id ?: "",
                    customId = user.customId,
                    email = user.email,
                    name = user.name,
                    profilePicUrl = user.profilePicUrl,
                    description = user.description,
                    weight = user.weight ?: 0.0,
                    height = user.height ?: 0.0,
                    age = user.age ?: 0,
                    gender = user.gender,
                    bmi = user.bmi ?: 0.0
                ))
            } catch (e: Exception) { }
        }
    }

    private fun saveChefToCache(chef: Chef) {
        viewModelScope.launch {
            val dao = getDao() ?: return@launch
            try {
                dao.insertChef(ChefEntity(
                    id = chef.id,
                    chefId = chef.chefId,
                    name = chef.name,
                    gender = chef.gender,
                    age = chef.age,
                    phoneNumber = chef.phoneNumber,
                    email = chef.email,
                    address = chef.address,
                    state = chef.state,
                    postcode = chef.postcode,
                    experience = chef.experience,
                    description = chef.description,
                    status = chef.status,
                    profilePictureUrl = chef.profilePictureUrl,
                    averagerating = chef.averagerating,
                    Pricing = chef.Pricing
                ))
            } catch (e: Exception) { }
        }
    }

    fun login(emailInput: String, passwordInput: String) {
        val cleanEmail = emailInput.trim()
        val cleanPassword = passwordInput.trim()

        if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
            errorMessage = "Email and password cannot be empty"
            return
        }

        if (cleanEmail == "admin@gmail.com" && cleanPassword == "admin1234") {
            isAdmin = true
            loginSuccess = true
            return
        }

        isProcessing = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                client.auth.signInWith(Email) {
                    email = cleanEmail
                    password = cleanPassword
                }

                val authUser = client.auth.currentUserOrNull() ?: throw Exception("Auth failed")
                val userId = authUser.id

                fetchUserData(userId)

                val chefData = client.postgrest["Chef"].select { filter { eq("chefId", userId) } }.decodeSingleOrNull<Chef>()

                if (chefData != null) {
                    when (chefData.status.lowercase()) {
                        "approved" -> {
                            this@AuthViewModel.currentChef = chefData
                            saveChefToCache(chefData)
                            isChef = true
                            isAdmin = false
                            loginSuccess = true
                        }
                        "pending" -> {
                            client.auth.signOut()
                            errorMessage = "Your chef application is pending admin approval."
                        }
                        else -> {
                            client.auth.signOut()
                            errorMessage = "Chef account is not active."
                        }
                    }
                    return@launch
                }

                if (this@AuthViewModel.currentUser != null) {
                    isChef = false
                    isAdmin = false
                    loginSuccess = true
                } else {
                    client.auth.signOut()
                    errorMessage = "Account details not found."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Login Failed"
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
                    errorMessage = "Registration successful! Please check your email."
                    isProcessing = false
                }
            } catch (e: Exception) {
                errorMessage = "Register Failed: ${e.message}"
                isProcessing = false
            }
        }
    }

    private fun setupNewUser(uid: String, email: String) {
        viewModelScope.launch {
            try {
                // 🌟 RESTORED ID logic: Fetch all users and calculate max numeric ID + 1
                val allUsers = client.postgrest.from("users").select().decodeList<User>()
                val maxIdNum = allUsers.mapNotNull { it.customId?.removePrefix("U")?.toIntOrNull() }.maxOrNull() ?: 0
                val customId = "U${(maxIdNum + 1).toString().padStart(3, '0')}"
                
                val newUser = User(id = uid, customId = customId, email = email, name = "User ($customId)")

                client.postgrest.from("users").insert(newUser)
                this@AuthViewModel.currentUser = newUser
                saveUserToCache(newUser)
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
                this@AuthViewModel.currentUser = user
                saveUserToCache(user)
            }
        } catch (e: Exception) { }
    }

    fun updateLocalChef(chef: Chef) {
        currentChef = chef
        saveChefToCache(chef)
    }

    fun fetchChefData() {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                val chefData = client.postgrest["Chef"].select { filter { eq("chefId", userId) } }.decodeSingleOrNull<Chef>()
                currentChef = chefData
                chefData?.let { saveChefToCache(it) }
            } catch (e: Exception) { }
        }
    }

    fun updateProfile(
        name: String, email: String, profilePicUrl: String, description: String = "",
        weight: Double? = null, height: Double? = null, age: Int? = null, gender: String? = null, bmi: Double? = null
    ) {
        val uid = currentUser?.id ?: return
        isProcessing = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                val updatedUser = currentUser?.copy(
                    name = name, email = email, profilePicUrl = profilePicUrl, description = description,
                    weight = weight ?: currentUser?.weight, height = height ?: currentUser?.height,
                    age = age ?: currentUser?.age, gender = gender ?: currentUser?.gender, bmi = bmi ?: currentUser?.bmi
                ) ?: return@launch
                
                client.postgrest.from("users").update(updatedUser) { filter { eq("id", uid) } }
                this@AuthViewModel.currentUser = updatedUser
                saveUserToCache(updatedUser)

                if (client.auth.currentUserOrNull()?.email != email && email.isNotEmpty()) {
                    client.auth.updateUser { this.email = email }
                }
                errorMessage = "Profile Updated"
            } catch (e: Exception) {
                errorMessage = "Update Failed"
            } finally {
                isProcessing = false
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        if (newPassword.length < 8) {
            errorMessage = "New password must be at least 8 characters"
            return
        }
        val email = currentUser?.email ?: return
        isProcessing = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                client.auth.signInWith(Email) { this.email = email; this.password = oldPassword }
                client.auth.updateUser { password = newPassword }
                errorMessage = "Password successfully changed"
            } catch (e: Exception) {
                errorMessage = "Failed to change password."
            } finally {
                isProcessing = false
            }
        }
    }

    fun updatePassword(newPassword: String) {
        if (newPassword.length < 8) {
            errorMessage = "Password must be at least 8 characters"
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
        currentUser = null
        currentChef = null
        isChef = false
        isAdmin = false
        loginSuccess = false
        registerSuccess = false
        errorMessage = ""
        viewModelScope.launch {
            try { 
                val dao = getDao()
                dao?.deleteUser()
                dao?.deleteChef()
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

    fun resetPasswordState() {
        errorMessage = ""
    }
}

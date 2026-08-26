package com.example.foodieheal.User.viewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Cloudinary.CloudinaryConfig
import com.example.foodieheal.MainActivity
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.User.local.UserDatabase
import com.example.foodieheal.User.local.ChefEntity
import com.example.foodieheal.User.local.UserDao
import com.example.foodieheal.User.local.UserEntity
import com.example.foodieheal.User.Model.User
import com.example.mobileassignmentloginpart.Model.Chef
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import androidx.lifecycle.ViewModelProvider

class AuthViewModel(private val networkMonitor: NetworkMonitor? = null) : ViewModel() {
    private val client = SupabaseClient.client

    private fun getDao(): UserDao? {
        return MainActivity.appContext?.let { UserDatabase.getDatabase(it).userDao() }
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

    var isNetworkAvailable by mutableStateOf(true)
        private set

    // 🌟 Temporary holders for registration data
    private var tempEmail = ""
    private var tempPassword = ""

    fun setTempCredentials(email: String, password: String) {
        tempEmail = email
        tempPassword = password
    }

    // 🌟 Validation: Check if email already exists in users table
    fun validateEmailUniqueness(emailInput: String, onSuccess: () -> Unit) {
        isProcessing = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                val cleanEmail = emailInput.trim()

                val userExists = client.postgrest.from("users").select {
                    filter { eq("email", cleanEmail) }
                }.decodeList<User>().isNotEmpty()

                if (userExists) {
                    errorMessage = "Email already registered"
                    isProcessing = false
                    return@launch
                }

                // If clear, proceed to the next step
                onSuccess()
            } catch (e: Exception) {
                errorMessage = parseError(e)
            } finally {
                isProcessing = false
            }
        }
    }

    var isProcessing by mutableStateOf(false)
        private set

    var isInitializing by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf("")
        private set

    var passwordErrorMessage by mutableStateOf("") // 🌟 Specific holder for password errors
        private set

    var profileMessage by mutableStateOf("") // 🌟 Specific holder for profile updates
        private set

    var CheferrorMessage by mutableStateOf<String?>(null)
        private set

    // 🌟 Professional One-Time Event System (Channel ensures the message only shows ONCE)
    sealed class ProfileEvent {
        object PasswordSuccess : ProfileEvent()
        object ProfileSuccess : ProfileEvent()
        object BodyStatusSuccess : ProfileEvent()
    }

    private val _profileEvents = Channel<ProfileEvent>(Channel.BUFFERED)
    val profileEvents = _profileEvents.receiveAsFlow()

    fun clearProfileEvents() {
        // Channels clear automatically when received, so this is mostly for safety
    }

    init {
        observeNetworkStatus()
        viewModelScope.launch {
            try {
                restoreOfflineSessionSync()

                if (currentUser != null || currentChef != null) {
                    loginSuccess = true
                    // Restore admin status if cached user is the admin
                    if (currentUser?.email == "admin@gmail.com") {
                        isAdmin = true
                    }
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
                delay(200)
                isInitializing = false
            }
        }
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor?.isConnected?.collect { connected ->
                isNetworkAvailable = connected
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
                dao.insertUser(
                    UserEntity(
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
                    )
                )
            } catch (e: Exception) { }
        }
    }

    private fun saveChefToCache(chef: Chef) {
        viewModelScope.launch {
            val dao = getDao() ?: return@launch
            try {
                dao.insertChef(
                    ChefEntity(
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
                    )
                )
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
                errorMessage = parseError(e)
            } finally {
                isProcessing = false
            }
        }
    }

    private fun parseError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("No address associated with hostname", ignoreCase = true) ||
            msg.contains("HttpRequestException", ignoreCase = true) ||
            msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("connection", ignoreCase = true) ->
                "Please connect to wifi or network connection"
            msg.contains("timeout", ignoreCase = true) ->
                "Connection timeout. Please try again."
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                "Invalid email or password"
            else -> msg.split("\n").firstOrNull() ?: "An error occurred"
        }
    }

    // 🌟 Unified Registration: Creates Auth + User Profile at once
    fun registerWithProfile(
        weight: Double?, height: Double?, age: Int?, gender: String, bmi: Double?
    ) {
        if (!isNetworkAvailable) {
            errorMessage = "No internet connection. Cannot register."
            return
        }
        if (tempEmail.isBlank() || tempPassword.isBlank()) {
            errorMessage = "Registration data lost. Please try again."
            return
        }

        isProcessing = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                // 1. Create Auth Account
                client.auth.signUpWith(Email) {
                    email = tempEmail
                    password = tempPassword
                }

                val authUser = client.auth.currentUserOrNull() ?: throw Exception("Auth failed")
                val uid = authUser.id

                // 2. Generate Custom ID
                val allUsers = client.postgrest.from("users").select().decodeList<User>()
                val maxIdNum = allUsers.mapNotNull { it.customId?.removePrefix("U")?.toIntOrNull() }.maxOrNull() ?: 0
                val customId = "U${(maxIdNum + 1).toString().padStart(3, '0')}"

                // 3. Create full profile including BMI info
                val newUser = User(
                    id = uid,
                    customId = customId,
                    email = tempEmail,
                    name = "User ($customId)",
                    weight = weight,
                    height = height,
                    age = age,
                    gender = gender,
                    bmi = bmi
                )

                client.postgrest.from("users").insert(newUser)

                // 4. Finalize
                this@AuthViewModel.currentUser = newUser
                saveUserToCache(newUser)
                loginSuccess = true

                // 🌟 Emit success event to trigger navigation and show message
                _profileEvents.send(ProfileEvent.BodyStatusSuccess)

                registerSuccess = true
            } catch (e: Exception) {
                errorMessage = "Registration Failed: ${parseError(e)}"
            } finally {
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

                val newUser =
                    User(id = uid, customId = customId, email = email, name = "User ($customId)")

                client.postgrest.from("users").insert(newUser)
                this@AuthViewModel.currentUser = newUser
                saveUserToCache(newUser)

                // 🌟 FIX: Explicitly set loginSuccess so the Bottom Navigation Bar appears immediately
                loginSuccess = true
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
        weight: Double? = null, height: Double? = null, age: Int? = null, gender: String? = null, bmi: Double? = null,
        imageBytes: ByteArray? = null, // 🌟 New parameter
        onSuccess: () -> Unit = {} // 🌟 Added callback for reliable navigation
    ) {
        if (!isNetworkAvailable) {
            profileMessage = "No internet connection. Cannot update profile."
            return
        }
        val uid = currentUser?.id ?: return
        isProcessing = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                var finalUrl = profilePicUrl

                // 1. Upload to Cloudinary if new image is provided
                if (imageBytes != null) {
                    uploadProfileImage(uid, imageBytes).onSuccess { url ->
                        finalUrl = url
                    }.onFailure { e ->
                        throw e
                    }
                }

                val updatedUser = currentUser?.copy(
                    name = name, email = email, profilePicUrl = finalUrl, description = description,
                    weight = weight ?: currentUser?.weight, height = height ?: currentUser?.height,
                    age = age ?: currentUser?.age, gender = gender ?: currentUser?.gender, bmi = bmi ?: currentUser?.bmi
                ) ?: return@launch

                client.postgrest.from("users").update(updatedUser) { filter { eq("id", uid) } }
                this@AuthViewModel.currentUser = updatedUser
                saveUserToCache(updatedUser)

                if (client.auth.currentUserOrNull()?.email != email && email.isNotEmpty()) {
                    client.auth.updateUser { this.email = email }
                }

                // 🌟 Send one-time success events
                if (weight != null || height != null || age != null || bmi != null) {
                    _profileEvents.send(ProfileEvent.BodyStatusSuccess)
                } else {
                    _profileEvents.send(ProfileEvent.ProfileSuccess)
                }

                errorMessage = ""
                profileMessage = "Profile Updated" // 🌟 Uses dedicated profile holder
                onSuccess()
            } catch (e: Exception) {
                profileMessage = "Update Failed: ${parseError(e)}"
            } finally {
                isProcessing = false
            }
        }
    }

    private suspend fun uploadProfileImage(uid: String, imageBytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                    .addFormDataPart(
                        "file",
                        "user_$uid.jpg",
                        imageBytes.toRequestBody("image/*".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Cloudinary failed: ${response.message}")
                    val responseBody = response.body?.string() ?: ""
                    val json = JSONObject(responseBody)
                    json.getString("secure_url")
                }
            }
        }

    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit = {}) {
        if (!isNetworkAvailable) {
            passwordErrorMessage = "No internet connection. Cannot change password."
            return
        }
        val email = currentUser?.email ?: return
        isProcessing = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                // 1. Always verify identity with OLD password first
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = oldPassword
                }

                // 2. ONLY proceed if the password is correct AND meets our 8-20 rule
                val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$".toRegex()
                if (!newPassword.matches(passwordRegex)) {
                    passwordErrorMessage = "Password must be 8-20 characters with uppercase, lowercase and numbers"
                    isProcessing = false
                    return@launch
                }

                // 3. Update to the NEW password
                client.auth.updateUser { password = newPassword }

                _profileEvents.send(ProfileEvent.PasswordSuccess)
                passwordErrorMessage = "" // 🌟 Clear errors on success
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                passwordErrorMessage = when { // 🌟 Uses dedicated password holder
                    msg.contains("Invalid login credentials", ignoreCase = true) -> "Invalid current password"
                    else -> "Failed to change password. ${parseError(e)}"
                }
            } finally {
                isProcessing = false
            }
        }
    }

    fun updatePassword(newPassword: String) {
        val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$".toRegex()
        if (!newPassword.matches(passwordRegex)) {
            errorMessage = "Password must be 8-20 characters with uppercase, lowercase and numbers"
            return
        }
        viewModelScope.launch {
            try {
                client.auth.updateUser { password = newPassword }
                errorMessage = "Profile Updated"
            } catch (e: Exception) {
                errorMessage = "Password update failed: ${parseError(e)}"
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
                errorMessage = "Failed to send reset email: ${parseError(e)}"
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

    // 🌟 Added to allow going back to Register screen to fix details
    fun resetRegisterState() {
        registerSuccess = false
    }

    fun resetPasswordState() {
        errorMessage = ""
        passwordErrorMessage = ""
        profileMessage = ""
    }

    class Factory(private val networkMonitor: NetworkMonitor) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(networkMonitor) as T
        }
    }
}
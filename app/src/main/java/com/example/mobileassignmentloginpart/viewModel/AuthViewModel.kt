package com.example.mobileassignmentloginpart.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.mobileassignmentloginpart.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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
        auth.currentUser?.let {
            fetchUserData(it.uid)
            loginSuccess = true
        }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage = "Email and password cannot be empty"
            return
        }
        isProcessing = true
        errorMessage = ""
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fetchUserData(task.result?.user?.uid ?: "")
                    loginSuccess = true
                } else {
                    errorMessage = task.exception?.message ?: "Login Failed"
                }
                isProcessing = false
            }
    }

    fun register(email: String, password: String) {
        isProcessing = true
        errorMessage = ""
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""
                    setupNewUser(uid, email)
                } else {
                    errorMessage = task.exception?.message ?: "Register Failed"
                    isProcessing = false
                }
            }
    }

    private fun setupNewUser(uid: String, email: String) {
        val metadataRef = firestore.collection("metadata").document("user_count")
        
        metadataRef.get().addOnSuccessListener { document ->
            val count = (document.getLong("count") ?: 0L) + 1L
            val customId = "U" + count.toString().padStart(5, '0')
            saveUserToFirestore(uid, customId, email, "User ($customId)", count)
        }.addOnFailureListener {
            // If metadata fails, we still try to create the user as U00001
            saveUserToFirestore(uid, "U00001", email, "User (U00001)", 1L)
        }
    }

    private fun saveUserToFirestore(uid: String, customId: String, email: String, name: String, count: Long) {
        val newUser = User(id = uid, customId = customId, email = email, name = name)
        
        firestore.collection("users").document(uid).set(newUser)
            .addOnSuccessListener {
                firestore.collection("metadata").document("user_count").set(mapOf("count" to count))
                currentUser = newUser
                registerSuccess = true
                isProcessing = false
            }
            .addOnFailureListener {
                errorMessage = "Database Error: ${it.message}"
                isProcessing = false
                // Even if firestore fails, we let them in so they can try "Update Profile" later
                currentUser = newUser
                registerSuccess = true
            }
    }

    fun fetchUserData(uid: String) {
        errorMessage = ""
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUser = document.toObject(User::class.java)
                } else {
                    // This user exists in Auth but not Firestore
                    currentUser = User(id = uid, customId = "Pending...", email = auth.currentUser?.email ?: "", name = "User")
                }
            }
            .addOnFailureListener {
                errorMessage = "Access Denied: ${it.message}"
                currentUser = User(id = uid, customId = "No Data", email = auth.currentUser?.email ?: "", name = "User")
            }
    }

    fun updateProfile(name: String, email: String, profilePicUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        isProcessing = true
        errorMessage = ""
        
        val cid = if (currentUser?.customId == "Pending..." || currentUser?.customId == "No Data") "U00001" else currentUser?.customId ?: "U00001"

        val updatedData = User(
            id = uid,
            customId = cid,
            name = name,
            email = email,
            profilePicUrl = profilePicUrl
        )

        firestore.collection("users").document(uid).set(updatedData, SetOptions.merge())
            .addOnSuccessListener {
                currentUser = updatedData
                val user = auth.currentUser
                if (user != null && user.email != email) {
                    user.updateEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                errorMessage = "Profile Updated"
                            } else {
                                if (task.exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                                    errorMessage = "Sensitive change. Please logout and login again to update email/password."
                                } else {
                                    errorMessage = "Saved, but email update failed: ${task.exception?.message}"
                                }
                            }
                            isProcessing = false
                        }
                } else {
                    errorMessage = "Profile Updated"
                    isProcessing = false
                }
            }
            .addOnFailureListener {
                errorMessage = "Save Failed: ${it.message}"
                isProcessing = false
            }
    }

    fun updatePassword(newPassword: String) {
        if (newPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }
        auth.currentUser?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    errorMessage = "Profile Updated"
                } else {
                    val exception = task.exception
                    if (exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        errorMessage = "Sensitive operation. Please logout and login again to update password."
                    } else {
                        errorMessage = exception?.message ?: "Password update failed"
                    }
                }
            }
    }

    fun logout() {
        auth.signOut()
        currentUser = null
        loginSuccess = false
        registerSuccess = false
        errorMessage = ""
    }
}

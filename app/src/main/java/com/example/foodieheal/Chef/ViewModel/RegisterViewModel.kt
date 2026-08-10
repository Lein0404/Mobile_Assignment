package com.example.foodieheal.Chef.ViewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Cloudinary.CloudinaryConfig
import com.example.foodieheal.Cloudinary.uploadImageToCloudinary
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.viewmodel.AuthViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID

class chefRegisterViewModel : ViewModel() {

    var name by mutableStateOf("")
    var gender by mutableStateOf("")
    var age by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var email by mutableStateOf("")
    var phoneNumber by mutableStateOf("")

    var address by mutableStateOf("")
    var state by mutableStateOf("")
        private set

    fun updateState(newState: String) {
        state = newState
    }

    var postcode by mutableStateOf("")

    var experience by mutableStateOf("")
    var description by mutableStateOf("")

    var isSubmitting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

// UPLOAD IMAGE FLOW
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    fun updateImage(uri: Uri?) {
        // OOP set
        selectedImageUri = uri
    }

    fun canProceedReviewPage(): Boolean {
        // Form validation before proceeding to the next page
        // Just ensure user have select image
        return selectedImageUri != null
    }

    fun registerChef(
        context: Context,
        onSuccess: () -> Unit
    ) {
        isSubmitting = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val client = SupabaseClient.client

               //Authenticate with Supabase first but my one is register so much thing to do
                try {
                    client.auth.signUpWith(Email) {
                        email = this@chefRegisterViewModel.email
                        password = this@chefRegisterViewModel.password
                    }
                } catch (signUpException: Exception) {
                    Log.w("ChefRegister", "signUpWith exception: ${signUpException.message}")
                    try {
                        client.auth.signInWith(Email) {
                            email = this@chefRegisterViewModel.email
                            password = this@chefRegisterViewModel.password
                        }
                    } catch (signInException: Exception) {
                        throw signUpException
                    }
                }

                var user = client.auth.currentUserOrNull()
                if (user == null) {
                    try {
                        client.auth.signInWith(Email) {
                            email = this@chefRegisterViewModel.email
                            password = this@chefRegisterViewModel.password
                        }
                        user = client.auth.currentUserOrNull()
                    } catch (e: Exception) {
                        Log.w("ChefRegister", "signInWith fallback failed: ${e.message}")
                    }
                }

                val authId = user?.id ?: UUID.randomUUID().toString()
                val customId = "C${(100..999).random()}"
                var imageUrl = ""

                // Upload image to Cloudinary if selected log just ignore la just view debug result only
                selectedImageUri?.let { uri ->
                    Log.d("ChefRegister", "Start Cloudinary upload: $uri")

                    // CALLING THE EXTENSION FUNCTION HERE !!!!!!!!!!!!!
                    // if no call god also cant save you
                    imageUrl = context.uploadImageToCloudinary(uri)

                    Log.d("ChefRegister", "Cloudinary URL: $imageUrl")
                }

                // This one just put the data class created to here and remember the image url variable
                val newChef = Chef(
                    id = customId,
                    chefId = authId,
                    name = name.trim(),
                    gender = gender,
                    age = age.trim().toIntOrNull() ?: 0,
                    phoneNumber = phoneNumber.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    state = state,
                    postcode = postcode.trim(),
                    experience = experience.trim().toIntOrNull() ?: 0,
                    description = description.trim(),
                    profilePictureUrl = imageUrl,
                    averagerating = null,
                    Pricing = null,
                    status = "Pending"
                )

                // Save to supabase that all
                client.postgrest.from("Chef").insert(newChef)

                client.auth.signOut()

                isSubmitting = false
                clearData()
                onSuccess()

                // can ingore these much log because I have many issue during the process
            } catch (e: Exception) {
                Log.e("ChefRegister", "Registration error trace", e)
                isSubmitting = false
                val msg = e.message ?: "Registration failed. Please try again."
                errorMessage = when {
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
        }
    }

    fun updateChefProfile(
        context: Context,
        updatedChef: Chef,
        newImageUri: Uri?,
        authViewModel: AuthViewModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val client = SupabaseClient.client
                var finalImageUrl = updatedChef.profilePictureUrl.orEmpty()

                // Upload new image using the extension function if selected
                newImageUri?.let { uri ->
                    finalImageUrl = context.uploadImageToCloudinary(uri)
                }

                val chefToSave = updatedChef.copy(
                    profilePictureUrl = finalImageUrl
                )

                // Update Supabase record safely using filter block
                client.postgrest.from("Chef").update(chefToSave) {
                    filter {
                        eq("chefId", chefToSave.chefId)
                    }
                }

                // Update AuthViewModel state
                authViewModel.updateLocalChef(chefToSave)

                onSuccess()
            } catch (e: Exception) {
                Log.e("ChefUpdate", "Error updating profile", e)
                onError(e.message ?: "Failed to update profile")
            }
        }
    }

    fun clearData() {
        name = ""
        gender = ""
        age = ""
        password = ""
        confirmPassword = ""

        email = ""
        phoneNumber = ""

        address = ""
        state = ""
        postcode = ""

        experience = ""
        description = ""
    }

    //Validation
    var showBasicInfoErrorMessage by mutableStateOf(false)
        private set

    var showContactErrorMessage by mutableStateOf(false)
        private set

    var showAddressErrorMessage by mutableStateOf(false)
        private set

    var showDescriptionErrorMessage by mutableStateOf(false)
        private set

    // All Basic Information Ui validation
    fun isValidName(): Boolean {
        return name.trim().length in 3..50 &&
                name.trim().matches(Regex("^[a-zA-Z ]+$"))
    }

    fun isValidAge(): Boolean {
        val ageInt = age.toIntOrNull()
        return ageInt != null &&
                ageInt in 18..100
    }

    fun isValidGender(): Boolean {
        return gender.isNotBlank()
    }

    fun isValidPassword(): Boolean {
        return password.length in 8..30 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }
    }

    fun isPasswordMatched(): Boolean {
        return password == confirmPassword
    }

    // All Contact Information Ui validation
    fun isValidEmail(): Boolean {
        return Patterns.EMAIL_ADDRESS
            .matcher(email)
            .matches()
    }

    fun isValidPhoneNumber(): Boolean {

        val phone = phoneNumber.trim()

        return phone.matches(
            Regex("^(01)[0-9]{8,9}$")
        )
    }


    //All address information Ui validation
    fun isValidAddress(): Boolean {

        val addressTrim = address.trim()

        return addressTrim.length in 10..200
    }

    fun isValidState(): Boolean {
        return state.isNotBlank()
    }

    fun isValidPostcode(): Boolean {

        return postcode.matches(
            Regex("^[0-9]{5}$")
        )
    }

    //all description information Ui validation
    fun isValidExperience(): Boolean {

        val experienceYear = experience.trim().toIntOrNull()

        return experienceYear != null &&
                experienceYear in 0..70
    }

    fun isValidDescription(): Boolean {
        return description.isNotBlank()
    }

    fun isValidProfilePicture(): Boolean {
     return selectedImageUri != null
    }

    // Next button enable feature
    fun canProceedBasicInfo(): Boolean {
        return name.isNotBlank() &&
                gender.isNotBlank() &&
                age.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank()
    }

    fun canProceedContactInfo(): Boolean {
        return email.isNotBlank() &&
                phoneNumber.isNotBlank()
    }

    fun canProceedAddressInfo(): Boolean {
        return address.isNotBlank() &&
                postcode.isNotBlank() &&
                state.isNotBlank()
    }

    fun canProceedDescriptionInfo(): Boolean {
        return experience.isNotBlank() &&
                description.isNotBlank()
    }

    // Validation of data field after click next button
    fun validateBasicInfo(): Boolean {
        showBasicInfoErrorMessage = true

        return isValidName() &&
                isValidGender() &&
                isValidAge() &&
                isValidPassword() &&
                isPasswordMatched()
    }

    fun validateContactInfo(): Boolean {

        showContactErrorMessage = true

        return isValidEmail() &&
                isValidPhoneNumber()
    }

    fun validateAddressInfo(): Boolean {

        showAddressErrorMessage = true

        return isValidAddress() &&
                isValidState() &&
                isValidPostcode()
    }

    fun validateDescriptionInfo(): Boolean {

        showDescriptionErrorMessage = true

        return isValidExperience() &&
                isValidDescription()
    }

    fun validateProfilePicture(){
        showDescriptionErrorMessage = true

        return
    }
}
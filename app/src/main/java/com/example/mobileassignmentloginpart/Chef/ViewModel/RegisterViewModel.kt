package com.example.mobileassignmentloginpart.Chef.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.mobileassignmentloginpart.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

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

    fun registerChef(onSuccess: () -> Unit) {
        isSubmitting = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val client = SupabaseClient.client

                try {
                    client.auth.signUpWith(Email) {
                        email = this@chefRegisterViewModel.email
                        password = this@chefRegisterViewModel.password
                    }
                } catch (signUpException: Exception) {
                    android.util.Log.w("ChefRegister", "signUpWith exception: ${signUpException.message}")
                    // If user is already registered in Auth, attempt signInWith
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
                        android.util.Log.w("ChefRegister", "signInWith fallback failed: ${e.message}")
                    }
                }

                val authId = user?.id ?: java.util.UUID.randomUUID().toString()

                val newChef = Chef(
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
                    status = "Pending"
                )

                //Insert record
                client.postgrest.from("Chef").insert(newChef)

                isSubmitting = false
                clearData()
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("ChefRegister", "Registration error trace", e)
                isSubmitting = false
                val msg = e.message ?: "Registration failed. Please try again."
                errorMessage = when {
                    msg.contains("row-level security", ignoreCase = true) || msg.contains("violates row-level security policy", ignoreCase = true) ->
                        "Supabase RLS Error: Please add an INSERT policy or disable RLS on the 'Chef' table in Supabase Dashboard."
                    msg.contains("already registered", ignoreCase = true) || msg.contains("already exists", ignoreCase = true) ->
                        "This email is already registered. Please login instead."
                    msg.contains("duplicate key", ignoreCase = true) || msg.contains("unique constraint", ignoreCase = true) ->
                        "Chef account already created. Please login."
                    else -> msg.lines().firstOrNull() ?: msg
                }
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
        return android.util.Patterns.EMAIL_ADDRESS
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
}
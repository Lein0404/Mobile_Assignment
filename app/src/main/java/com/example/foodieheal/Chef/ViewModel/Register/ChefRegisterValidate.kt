package com.example.foodieheal.Chef.ViewModel.Register

import android.net.Uri
import android.util.Patterns

object ChefRegisterValidate {

    fun isValidName(name: String): Boolean {
        return name.trim().length in 3..50 &&
                name.trim().matches(Regex("^[a-zA-Z ]+$"))
    }

    fun isValidAge(age: String): Boolean {
        val ageInt = age.toIntOrNull()
        return ageInt != null && ageInt in 18..100
    }

    fun isValidGender(gender: String): Boolean {
        return gender.isNotBlank()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length in 8..30 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }
    }

    fun isPasswordMatched(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.trim().matches(Regex("^(01)[0-9]{8,9}$"))
    }

    fun isValidAddress(address: String): Boolean {
        return address.trim().length in 10..200
    }

    fun isValidState(state: String): Boolean {
        return state.isNotBlank()
    }

    fun isValidPostcode(postcode: String): Boolean {
        return postcode.matches(Regex("^[0-9]{5}$"))
    }

    fun isValidExperience(experience: String): Boolean {
        val experienceYear = experience.trim().toIntOrNull()
        return experienceYear != null && experienceYear in 0..70
    }

    fun isValidDescription(description: String): Boolean {
        return description.isNotBlank()
    }

    fun isValidProfilePicture(selectedImageUri: Uri?): Boolean {
        return selectedImageUri != null
    }

    fun canProceedBasicInfo(
        name: String,
        gender: String,
        age: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return name.isNotBlank() &&
                gender.isNotBlank() &&
                age.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank()
    }

    fun canProceedContactInfo(email: String, phoneNumber: String): Boolean {
        return email.isNotBlank() && phoneNumber.isNotBlank()
    }

    fun canProceedAddressInfo(address: String, postcode: String, state: String): Boolean {
        return address.isNotBlank() && postcode.isNotBlank() && state.isNotBlank()
    }

    fun canProceedDescriptionInfo(experience: String, description: String): Boolean {
        return experience.isNotBlank() && description.isNotBlank()
    }

    fun validateBasicInfo(
        name: String,
        gender: String,
        age: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return isValidName(name) &&
                isValidGender(gender) &&
                isValidAge(age) &&
                isValidPassword(password) &&
                isPasswordMatched(password, confirmPassword)
    }

    fun validateContactInfo(email: String, phoneNumber: String): Boolean {
        return isValidEmail(email) && isValidPhoneNumber(phoneNumber)
    }

    fun validateAddressInfo(address: String, state: String, postcode: String): Boolean {
        return isValidAddress(address) && isValidState(state) && isValidPostcode(postcode)
    }

    fun validateDescriptionInfo(experience: String, description: String): Boolean {
        return isValidExperience(experience) && isValidDescription(description)
    }
}

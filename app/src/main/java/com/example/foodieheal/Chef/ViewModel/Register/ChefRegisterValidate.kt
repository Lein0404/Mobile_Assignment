package com.example.foodieheal.Chef.ViewModel.Register

import android.net.Uri
import android.util.Patterns
import com.example.foodieheal.R

object ChefRegisterValidate {

    fun isValidName(name: String): Boolean {
        return name.trim().length in 3..50 &&
                name.trim().matches(Regex("^[a-zA-Z ]+$"))
    }

    fun getNameErrorRes(name: String): Int? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> R.string.ingredients_error_name_required
            trimmed.length < 3 || trimmed.length > 50 || !trimmed.matches(Regex("^[a-zA-Z ]+$")) -> R.string.error_name_invalid
            else -> null
        }
    }

    fun isValidAge(age: String): Boolean {
        val ageInt = age.toIntOrNull()
        return ageInt != null && ageInt in 18..100
    }

    fun getAgeErrorRes(age: String): Int? {
        val trimmed = age.trim()
        val ageInt = trimmed.toIntOrNull()
        return when {
            trimmed.isEmpty() -> R.string.error_age_required
            ageInt == null || ageInt !in 18..100 -> R.string.error_age_range
            else -> null
        }
    }

    fun isValidGender(gender: String): Boolean {
        return gender.isNotBlank()
    }

    fun getGenderErrorRes(gender: String): Int? {
        return if (gender.isBlank()) R.string.error_gender_required else null
    }

    fun isValidPassword(password: String): Boolean {
        return password.length in 8..30 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }
    }

    fun getPasswordErrorRes(password: String): Int? {
        return when {
            password.isEmpty() -> R.string.error_password_required
            !isValidPassword(password) -> R.string.error_chef_password_rule
            else -> null
        }
    }

    fun isPasswordMatched(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    fun getConfirmPasswordErrorRes(password: String, confirmPassword: String): Int? {
        return when {
            confirmPassword.isEmpty() -> R.string.error_confirm_password_required
            !isPasswordMatched(password, confirmPassword) -> R.string.error_passwords_not_matching
            else -> null
        }
    }

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    fun getEmailErrorRes(email: String): Int? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_email_required
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> R.string.error_email_invalid
            else -> null
        }
    }

    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.trim().matches(Regex("^(01)[0-9]{8,9}$"))
    }

    fun getPhoneNumberErrorRes(phoneNumber: String): Int? {
        val trimmed = phoneNumber.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_phone_required
            !isValidPhoneNumber(trimmed) -> R.string.error_phone_invalid
            else -> null
        }
    }

    fun isValidAddress(address: String): Boolean {
        return address.trim().length in 10..200
    }

    fun getAddressErrorRes(address: String): Int? {
        val trimmed = address.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_address_required
            trimmed.length < 10 -> R.string.error_address_invalid
            else -> null
        }
    }

    fun isValidState(state: String): Boolean {
        return state.isNotBlank()
    }

    fun getStateErrorRes(state: String): Int? {
        return if (state.isBlank()) R.string.error_state_required else null
    }

    fun isValidPostcode(postcode: String): Boolean {
        return postcode.matches(Regex("^[0-9]{5}$"))
    }

    fun getPostcodeErrorRes(postcode: String): Int? {
        val trimmed = postcode.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_postcode_required
            !isValidPostcode(trimmed) -> R.string.error_postcode_invalid
            else -> null
        }
    }

    fun isValidExperience(experience: String): Boolean {
        val experienceYear = experience.trim().toIntOrNull()
        return experienceYear != null && experienceYear in 0..70
    }

    fun getExperienceErrorRes(experience: String): Int? {
        val trimmed = experience.trim()
        val exp = trimmed.toIntOrNull()
        return when {
            trimmed.isEmpty() -> R.string.error_experience_required
            exp == null || exp !in 0..70 -> R.string.error_experience_invalid
            else -> null
        }
    }

    fun isValidDescription(description: String): Boolean {
        val words = description.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        return description.isNotBlank() && words <= 300
    }

    fun getDescriptionErrorRes(description: String): Int? {
        val trimmed = description.trim()
        val words = if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        return when {
            trimmed.isEmpty() -> R.string.ingredients_error_description_required
            words > 300 -> R.string.error_description_limit
            else -> null
        }
    }

    fun isValidProfilePicture(selectedImageUri: Uri?): Boolean {
        return selectedImageUri != null
    }

    fun getProfilePictureErrorRes(selectedImageUri: Uri?): Int? {
        return if (selectedImageUri == null) R.string.error_profile_picture_required else null
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

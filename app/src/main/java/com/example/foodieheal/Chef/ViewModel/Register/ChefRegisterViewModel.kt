package com.example.foodieheal.Chef.ViewModel.Register

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.mobileassignmentloginpart.Model.Chef
import kotlinx.coroutines.launch

class ChefRegisterViewModel(
    private val repository: ChefRegisterRepository = ChefRegisterRepository()
) : ViewModel() {

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

    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    var showBasicInfoErrorMessage by mutableStateOf(false)
        private set

    var showContactErrorMessage by mutableStateOf(false)
        private set

    var showAddressErrorMessage by mutableStateOf(false)
        private set

    var showDescriptionErrorMessage by mutableStateOf(false)
        private set

    var showProfilePictureErrorMessage by mutableStateOf(false)
        private set

    // Error Resource Resolvers for UI consumption
    val nameErrorRes: Int?
        get() = if (showBasicInfoErrorMessage) ChefRegisterValidate.getNameErrorRes(name) else null

    val genderErrorRes: Int?
        get() = if (showBasicInfoErrorMessage) ChefRegisterValidate.getGenderErrorRes(gender) else null

    val ageErrorRes: Int?
        get() = if (showBasicInfoErrorMessage) ChefRegisterValidate.getAgeErrorRes(age) else null

    val passwordErrorRes: Int?
        get() = if (showBasicInfoErrorMessage) ChefRegisterValidate.getPasswordErrorRes(password) else null

    val confirmPasswordErrorRes: Int?
        get() = if (showBasicInfoErrorMessage) ChefRegisterValidate.getConfirmPasswordErrorRes(password, confirmPassword) else null

    val emailErrorRes: Int?
        get() = if (showContactErrorMessage) ChefRegisterValidate.getEmailErrorRes(email) else null

    val phoneErrorRes: Int?
        get() = if (showContactErrorMessage) ChefRegisterValidate.getPhoneNumberErrorRes(phoneNumber) else null

    val addressErrorRes: Int?
        get() = if (showAddressErrorMessage) ChefRegisterValidate.getAddressErrorRes(address) else null

    val postcodeErrorRes: Int?
        get() = if (showAddressErrorMessage) ChefRegisterValidate.getPostcodeErrorRes(postcode) else null

    val stateErrorRes: Int?
        get() = if (showAddressErrorMessage) ChefRegisterValidate.getStateErrorRes(state) else null

    val experienceErrorRes: Int?
        get() = if (showDescriptionErrorMessage) ChefRegisterValidate.getExperienceErrorRes(experience) else null

    val descriptionErrorRes: Int?
        get() = if (showDescriptionErrorMessage) ChefRegisterValidate.getDescriptionErrorRes(description) else null

    val profilePictureErrorRes: Int?
        get() = if (showProfilePictureErrorMessage) ChefRegisterValidate.getProfilePictureErrorRes(selectedImageUri) else null

    fun updateImage(uri: Uri?) {
        selectedImageUri = uri
        if (showProfilePictureErrorMessage && uri != null) {
            showProfilePictureErrorMessage = false
        }
    }

    fun canProceedReviewPage(): Boolean {
        return ChefRegisterValidate.isValidProfilePicture(selectedImageUri)
    }

    fun registerChef(context: Context, onSuccess: () -> Unit) {
        isSubmitting = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val authId = repository.authenticateUser(
                    email = email.trim(),
                    password = password
                )

                var imageUrl = ""
                selectedImageUri?.let { uri ->
                    imageUrl = repository.uploadProfileImage(context, uri)
                }

                val newChef = Chef(
                    id = repository.generateCustomChefId(),
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

                repository.insertChef(newChef)
                repository.signOut()

                isSubmitting = false
                clearData()
                onSuccess()
            } catch (e: Exception) {
                Log.e("ChefRegister", "Registration error trace", e)
                isSubmitting = false
                errorMessage = repository.mapRegistrationError(e.message)
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
                var finalImageUrl = updatedChef.profilePictureUrl.orEmpty()

                newImageUri?.let { uri ->
                    finalImageUrl = repository.uploadProfileImage(context, uri)
                }

                val chefToSave = updatedChef.copy(profilePictureUrl = finalImageUrl)
                repository.updateChef(chefToSave)
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
        selectedImageUri = null
    }

    fun isValidName(): Boolean = ChefRegisterValidate.isValidName(name)
    fun isValidAge(): Boolean = ChefRegisterValidate.isValidAge(age)
    fun isValidGender(): Boolean = ChefRegisterValidate.isValidGender(gender)
    fun isValidPassword(): Boolean = ChefRegisterValidate.isValidPassword(password)
    fun isPasswordMatched(): Boolean =
        ChefRegisterValidate.isPasswordMatched(password, confirmPassword)
    fun isValidEmail(): Boolean = ChefRegisterValidate.isValidEmail(email)
    fun isValidPhoneNumber(): Boolean = ChefRegisterValidate.isValidPhoneNumber(phoneNumber)
    fun isValidAddress(): Boolean = ChefRegisterValidate.isValidAddress(address)
    fun isValidState(): Boolean = ChefRegisterValidate.isValidState(state)
    fun isValidPostcode(): Boolean = ChefRegisterValidate.isValidPostcode(postcode)
    fun isValidExperience(): Boolean = ChefRegisterValidate.isValidExperience(experience)
    fun isValidDescription(): Boolean = ChefRegisterValidate.isValidDescription(description)
    fun isValidProfilePicture(): Boolean =
        ChefRegisterValidate.isValidProfilePicture(selectedImageUri)

    fun canProceedBasicInfo(): Boolean = ChefRegisterValidate.canProceedBasicInfo(
        name, gender, age, password, confirmPassword
    )

    fun canProceedContactInfo(): Boolean =
        ChefRegisterValidate.canProceedContactInfo(email, phoneNumber)

    fun canProceedAddressInfo(): Boolean =
        ChefRegisterValidate.canProceedAddressInfo(address, postcode, state)

    fun canProceedDescriptionInfo(): Boolean =
        ChefRegisterValidate.canProceedDescriptionInfo(experience, description)

    fun validateBasicInfo(): Boolean {
        showBasicInfoErrorMessage = true
        return ChefRegisterValidate.validateBasicInfo(
            name, gender, age, password, confirmPassword
        )
    }

    fun validateContactInfo(): Boolean {
        showContactErrorMessage = true
        return ChefRegisterValidate.validateContactInfo(email, phoneNumber)
    }

    fun validateAddressInfo(): Boolean {
        showAddressErrorMessage = true
        return ChefRegisterValidate.validateAddressInfo(address, state, postcode)
    }

    fun validateDescriptionInfo(): Boolean {
        showDescriptionErrorMessage = true
        return ChefRegisterValidate.validateDescriptionInfo(experience, description)
    }

    fun validateProfilePicture(): Boolean {
        showProfilePictureErrorMessage = true
        return ChefRegisterValidate.isValidProfilePicture(selectedImageUri)
    }

    fun resetRegistrationFlow() {
        clearData()
        showBasicInfoErrorMessage = false
        showContactErrorMessage = false
        showAddressErrorMessage = false
        showDescriptionErrorMessage = false
        showProfilePictureErrorMessage = false
        errorMessage = null
        selectedImageUri = null
    }
}

package com.example.mobileassignmentloginpart.Chef.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

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
}
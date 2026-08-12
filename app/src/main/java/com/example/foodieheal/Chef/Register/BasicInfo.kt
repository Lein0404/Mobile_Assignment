package com.example.foodieheal.Chef.Register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel
import com.example.foodieheal.R
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.foodieheal.ui.components.CommonInputField
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.ui.components.PasswordInputField

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun basicInfo(
    navController: NavController,
    chefViewModel: chefRegisterViewModel
) {
    val isNameError = chefViewModel.showBasicInfoErrorMessage && !chefViewModel.isValidName()
    val isGenderError = chefViewModel.showBasicInfoErrorMessage && !chefViewModel.isValidGender()
    val isAgeError = chefViewModel.showBasicInfoErrorMessage && !chefViewModel.isValidAge()
    val isPasswordError = chefViewModel.showBasicInfoErrorMessage && !chefViewModel.isValidPassword()
    val isConfirmPasswordError = chefViewModel.showBasicInfoErrorMessage &&
            (chefViewModel.confirmPassword.isBlank() || !chefViewModel.isPasswordMatched())
    val viewModel: AuthViewModel = viewModel()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Basic Information") },

                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrowback),
                                contentDescription = "Back"
                            )
                        }
                    }
                )

                LinearProgressIndicator(
                    progress = { 0.2f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Step 1 of 4",
                style = MaterialTheme.typography.titleMedium
            )

            CommonInputField(
                value = chefViewModel.name,
                onValueChange = { chefViewModel.name = it },
                textId = R.string.full_name,
                placeholder = stringResource(R.string.enter_name),
                isError = isNameError,
                supportingText = if (isNameError) {
                    { Text("Name cannot be empty.") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            DropDownList(
                labelId = R.string.gender,
                placeholderId = R.string.select_gender,
                selectedValue = chefViewModel.gender,
                options = listOf("Male", "Female"),
                onOptionSelected = { chefViewModel.gender = it ?: "" }
            )
            if (isGenderError) {
                Text(
                    text = "Please select your gender.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            CommonInputField(
                value = chefViewModel.age,
                onValueChange = { input ->
                    chefViewModel.age = input.filter { it.isDigit() }
                },
                textId = R.string.age,
                placeholder = stringResource(R.string.enter_age),
                isError = isAgeError,
                supportingText = if (isAgeError) {
                    { Text("Chef must be between 18 and 100 years old.") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            PasswordInputField(
                value = chefViewModel.password,
                onValueChange = { chefViewModel.password = it },
                textId = R.string.password,
                placeholder = stringResource(R.string.password),
                isError = isPasswordError,
                supportingText = if (isPasswordError) {
                    { Text("Password needs 8 chars, 1 uppercase, 1 lowercase, and 1 number.") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            PasswordInputField(
                value = chefViewModel.confirmPassword,
                onValueChange = { chefViewModel.confirmPassword = it },
                textId = R.string.confirm_password,
                placeholder = stringResource(R.string.confirm_password),
                isError = isConfirmPasswordError,
                supportingText = if (isConfirmPasswordError) {
                    {
                        val errorText = if (chefViewModel.confirmPassword.isBlank()) {
                            "Please confirm your password."
                        } else {
                            "Passwords do not match."
                        }
                        Text(errorText)
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Validate input
                    // Save data to ViewModel
                    if (chefViewModel.validateBasicInfo()) {
                        navController.navigate("contactInfo")
                    }
                },
                enabled = chefViewModel.canProceedBasicInfo(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }
        }
    }
}
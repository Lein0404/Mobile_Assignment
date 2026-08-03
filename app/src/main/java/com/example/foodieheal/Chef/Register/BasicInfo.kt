package com.example.foodieheal.Chef.Register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    chefviewModel: chefRegisterViewModel
) {

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
                value = chefviewModel.name,
                onValueChange = {
                    chefviewModel.name = it
                },
                textId = R.string.full_name,
                placeholder = stringResource(R.string.enter_name),
                modifier = Modifier.fillMaxWidth()
            )
            if (
                chefviewModel.showBasicInfoErrorMessage &&
                !chefviewModel.isValidName()
            ) {
                Text(
                    text = "Name is required.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            DropDownList(
                labelId = R.string.gender,
                placeholderId = R.string.select_gender,
                selectedValue = chefviewModel.gender,
                options = listOf(
                    "Male",
                    "Female"
                ),
                onOptionSelected = {
                    chefviewModel.gender = it ?: ""
                }
            )
            if (chefviewModel.showBasicInfoErrorMessage &&
                !chefviewModel.isValidGender()) {
                Text(
                    text = "Please select your gender.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            CommonInputField(
                value = chefviewModel.age,
                onValueChange = {
                    chefviewModel.age = it
                },
                textId = R.string.age,
                placeholder = stringResource(R.string.enter_age),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                        )
            )
            if (chefviewModel.showBasicInfoErrorMessage &&
                chefviewModel.age.isNotEmpty() &&
                !chefviewModel.isValidAge()) {
                Text(
                    text = "Chef must be at least 18 years old.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PasswordInputField(
                value = chefviewModel.password,
                onValueChange = { chefviewModel.password = it },
                textId = R.string.password,
                placeholder = stringResource(R.string.password),
                modifier = Modifier.fillMaxWidth()
            )
            if (chefviewModel.showBasicInfoErrorMessage &&
                chefviewModel.password.isNotEmpty()
                && !chefviewModel.isValidPassword()) {
                Text(
                    text = "Password must contain at least 8 characters, one uppercase letter, one lowercase letter and one number.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PasswordInputField(
                value = chefviewModel.confirmPassword,
                onValueChange = { chefviewModel.confirmPassword = it },
                textId = R.string.confirm_password,
                placeholder = stringResource(R.string.confirm_password),
                modifier = Modifier.fillMaxWidth()
            )
            if (chefviewModel.showBasicInfoErrorMessage &&
                chefviewModel.confirmPassword.isBlank()
            ) {
                Text(
                    text = "Please confirm your password.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (chefviewModel.showBasicInfoErrorMessage &&
                chefviewModel.confirmPassword.isNotBlank() &&
                !chefviewModel.isPasswordMatched()
            ) {
                Text(
                    text = "Passwords do not match.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Validate input
                    // Save data to ViewModel
                    if (chefviewModel.validateBasicInfo()) {
                        navController.navigate("contactInfo")
                    }
                },
                enabled = chefviewModel.canProceedBasicInfo(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }
        }
    }
}
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun contactInfo(
    navController: NavController,
    chefViewModel: chefRegisterViewModel
) {
    val viewModel: AuthViewModel = viewModel()
    val isEmailError = chefViewModel.showContactErrorMessage && !chefViewModel.isValidEmail()
    val isPhoneError = chefViewModel.showContactErrorMessage && !chefViewModel.isValidPhoneNumber()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Contact Information") },

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
                text = "Step 2 of 4",
                style = MaterialTheme.typography.titleMedium
            )

            CommonInputField(
                value = chefViewModel.email,
                onValueChange = { chefViewModel.email = it.trim() },
                textId = R.string.email,
                placeholder = stringResource(R.string.email),
                isError = isEmailError,
                supportingText = if (isEmailError) {
                    { Text("Please enter a valid email address.") }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier.fillMaxWidth()
            )

            CommonInputField(
                value = chefViewModel.phoneNumber,
                onValueChange = { input ->
                    chefViewModel.phoneNumber = input.filter { it.isDigit() }
                },
                textId = R.string.phone_number,
                placeholder = stringResource(R.string.phone_number),
                isError = isPhoneError,
                supportingText = if (isPhoneError) {
                    { Text("Please enter a valid phone number (e.g., 0123456789).") }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Validate input
                    // Save data to ViewModel
                    if (chefViewModel.validateContactInfo()) {
                        navController.navigate("addressInfo")
                    }
                },
                enabled = chefViewModel.canProceedContactInfo(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }
        }
    }
}
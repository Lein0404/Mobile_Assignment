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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun contactInfo(
    navController: NavController,
    chefviewModel: chefRegisterViewModel
) {
    val viewModel: AuthViewModel = viewModel()

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
                value = chefviewModel.email,
                onValueChange = {
                    chefviewModel.email = it
                },
                textId = R.string.email,
                placeholder = stringResource(R.string.email),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (
                chefviewModel.showContactErrorMessage &&
                !chefviewModel.isValidEmail()
            ) {
                Text(
                    text = "Please enter a valid email address.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            CommonInputField(
                value = chefviewModel.phoneNumber,
                onValueChange = {
                    chefviewModel.phoneNumber = it
                },
                textId = R.string.phone_number,
                placeholder = stringResource(R.string.phone_number),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (
                chefviewModel.showContactErrorMessage &&
                !chefviewModel.isValidPhoneNumber()
            ) {
                Text(
                    text = "Please enter a valid phone number.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Validate input
                    // Save data to ViewModel
                    if (chefviewModel.validateContactInfo()) {
                        navController.navigate("addressInfo")
                    }
                },
                enabled = chefviewModel.canProceedContactInfo(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }
        }
    }
}
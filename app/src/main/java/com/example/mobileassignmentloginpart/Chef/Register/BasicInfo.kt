package com.example.mobileassignmentloginpart.Chef.Register

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobileassignmentloginpart.Chef.ViewModel.chefRegisterViewModel
import com.example.mobileassignmentloginpart.R
import com.example.mobileassignmentloginpart.ViewModel.AuthViewModel
import com.example.mobileassignmentloginpart.ui.components.CommonInputField
import com.example.mobileassignmentloginpart.ui.components.DropDownList
import com.example.mobileassignmentloginpart.ui.components.PasswordInputField

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
                    progress = { 0.25f },
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
                text = "Create your chef profile",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Personal Information",
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

            PasswordInputField(
                value = chefviewModel.password,
                onValueChange = { chefviewModel.password = it },
                textId = R.string.password,
                placeholder = stringResource(R.string.password),
                modifier = Modifier.fillMaxWidth()
            )

            PasswordInputField(
                value = chefviewModel.confirmPassword,
                onValueChange = { chefviewModel.confirmPassword = it },
                textId = R.string.confirm_password,
                placeholder = stringResource(R.string.confirm_password),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    // Validate input
                    // Save data to ViewModel
                    // Navigate to Contact Info page
                    navController.navigate("contactInfo")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }

        }
    }
}
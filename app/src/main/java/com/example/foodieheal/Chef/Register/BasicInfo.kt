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
import androidx.navigation.NavController
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.CommonInputField
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.ui.components.PasswordInputField

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun basicInfo(
    navController: NavController,
    chefViewModel: ChefRegisterViewModel
) {
    val nameError = chefViewModel.nameErrorRes?.let { stringResource(it) }
    val genderError = chefViewModel.genderErrorRes?.let { stringResource(it) }
    val ageError = chefViewModel.ageErrorRes?.let { stringResource(it) }
    val passwordError = chefViewModel.passwordErrorRes?.let { stringResource(it) }
    val confirmPasswordError = chefViewModel.confirmPasswordErrorRes?.let { stringResource(it) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_basic_info)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrowback),
                                contentDescription = stringResource(R.string.back)
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
                text = stringResource(R.string.step_1_of_5),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            CommonInputField(
                value = chefViewModel.name,
                onValueChange = { chefViewModel.name = it },
                textId = R.string.full_name,
                placeholder = stringResource(R.string.enter_name),
                isError = nameError != null,
                supportingText = nameError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth()
            )

            DropDownList(
                labelId = R.string.gender,
                placeholderId = R.string.select_gender,
                selectedValue = chefViewModel.gender,
                options = listOf("Male", "Female"),
                onOptionSelected = { chefViewModel.gender = it ?: "" }
            )
            if (genderError != null) {
                Text(
                    text = genderError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            CommonInputField(
                value = chefViewModel.age,
                onValueChange = { input ->
                    chefViewModel.age = input.filter { it.isDigit() }.take(3)
                },
                textId = R.string.age,
                placeholder = stringResource(R.string.enter_age),
                isError = ageError != null,
                supportingText = ageError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            PasswordInputField(
                value = chefViewModel.password,
                onValueChange = { chefViewModel.password = it },
                textId = R.string.password,
                placeholder = stringResource(R.string.password),
                isError = passwordError != null,
                supportingText = passwordError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth()
            )

            PasswordInputField(
                value = chefViewModel.confirmPassword,
                onValueChange = { chefViewModel.confirmPassword = it },
                textId = R.string.confirm_password,
                placeholder = stringResource(R.string.confirm_password),
                isError = confirmPasswordError != null,
                supportingText = confirmPasswordError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (chefViewModel.validateBasicInfo()) {
                        navController.navigate("contactInfo")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.next))
            }
        }
    }
}
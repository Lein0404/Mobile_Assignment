package com.example.mobileassignmentloginpart.Chef

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mobileassignmentloginpart.ui.components.PasswordInputField
import com.example.mobileassignmentloginpart.R
import com.example.mobileassignmentloginpart.ui.components.CommonInputField
import com.example.mobileassignmentloginpart.ui.components.DropDownList
import com.example.mobileassignmentloginpart.ui.components.GenderDropdown

@Composable
fun ChefRegister(navController: NavController) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var phoneNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    var experience by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Chef Registration",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Create your chef profile",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleMedium
        )

        CommonInputField(
            value = name,
            onValueChange = {
                name = it
            },
            textId = R.string.full_name,
            placeholder = stringResource(R.string.full_name),
            modifier = Modifier.fillMaxWidth()
        )


        CommonInputField(
            value = email,
            onValueChange = {
                email = it
            },
            textId = R.string.email,
            placeholder = stringResource(R.string.email),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth()
        )


        CommonInputField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
            },
            textId = R.string.phone_number,
            placeholder = stringResource(R.string.phone_number),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            modifier = Modifier.fillMaxWidth()
        )

        DropDownList(
            labelId = R.string.gender,
            placeholderId = R.string.select_gender,
            selectedValue = gender,
            options = listOf(
                "Male",
                "Female"
            ),
            onOptionSelected = {
                gender = it ?: ""
            }
        )

        Text(
            text = "Security",
            style = MaterialTheme.typography.titleMedium
        )

        PasswordInputField(
            value = password,
            onValueChange = { password = it },
            textId = R.string.password,
            placeholder = stringResource(R.string.password),
            modifier = Modifier.fillMaxWidth()
        )

        PasswordInputField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            textId = R.string.confirm_password,
            placeholder = stringResource(R.string.confirm_password),
            modifier = Modifier.fillMaxWidth()
        )

        // Chef info
        Text(
            text = "Chef Profile",
            style = MaterialTheme.typography.titleMedium
        )

        CommonInputField(
            value = experience,
            onValueChange = { input ->

                if (input.all { it.isDigit() }) {
                    experience = input
                }

            },
            textId = R.string.experience,
            placeholder = stringResource(R.string.experience),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            CommonInputField(
                value = description,
                onValueChange = { input ->

                    val wordCount = input.trim()
                        .split("\\s+".toRegex())
                        .filter { it.isNotEmpty() }
                        .size

                    if (wordCount <= 300) {
                        description = input
                    }

                },
                textId = R.string.description,
                placeholder = stringResource(R.string.description),
                singleLine = false,
                minLines = 5,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )


            Text(
                text = "${description.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size}/300 words",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Button(
            onClick = {
                // Register done or fail after verify by supabase
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Register as Chef"
            )
        }

        TextButton(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text("Already have account? Login")
        }
    }
}
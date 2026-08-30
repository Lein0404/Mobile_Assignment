package com.example.foodieheal.Chef.Register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun contactInfo(
    navController: NavController,
    chefViewModel: ChefRegisterViewModel
) {
    val emailError = chefViewModel.emailErrorRes?.let { stringResource(it) }
    val phoneError = chefViewModel.phoneErrorRes?.let { stringResource(it) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_contact_info)) },
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
                    progress = { 0.4f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.step_2_of_5),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (chefViewModel.isUpgradeFlow) {
                CommonInputField(
                    value = chefViewModel.email,
                    onValueChange = { },
                    textId = R.string.email,
                    placeholder = stringResource(R.string.email),
                    isError = false,
                    enabled = false,
                    supportingText = {
                        Text(
                            stringResource(R.string.chef_contact_account_email_reused),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CommonInputField(
                    value = chefViewModel.email,
                    onValueChange = {
                        chefViewModel.email = it.trim()
                        chefViewModel.clearEmailTakenError()
                    },
                    textId = R.string.email,
                    placeholder = stringResource(R.string.email),
                    isError = emailError != null,
                    supportingText = emailError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            CommonInputField(
                value = chefViewModel.phoneNumber,
                onValueChange = { input ->
                    chefViewModel.phoneNumber = input.filter { it.isDigit() }.take(11)
                },
                textId = R.string.phone_number,
                placeholder = stringResource(R.string.phone_number),
                isError = phoneError != null,
                supportingText = phoneError?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    chefViewModel.validateContactInfo {
                        navController.navigate("addressInfo")
                    }
                },
                enabled = !chefViewModel.isCheckingContact,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (chefViewModel.isCheckingContact) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(stringResource(R.string.next))
                }
            }
        }
    }
}
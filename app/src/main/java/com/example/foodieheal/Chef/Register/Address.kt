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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.foodieheal.Chef.States
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.CommonInputField
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addressInfo(
    navController: NavController,
    chefViewModel: chefRegisterViewModel
) {

    val isAddressError = chefViewModel.showAddressErrorMessage && !chefViewModel.isValidAddress()
    val isPostcodeError = chefViewModel.showAddressErrorMessage && !chefViewModel.isValidPostcode()
    val isStateError = chefViewModel.showAddressErrorMessage && !chefViewModel.isValidState()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Address Information") },

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
                    progress = { 0.6f },
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
        ){
            Text(
                text = "Step 3 of 4",
                style = MaterialTheme.typography.titleMedium
            )

            CommonInputField(
                value = chefViewModel.address,
                onValueChange = { chefViewModel.address = it },
                textId = R.string.address,
                placeholder = "Enter your address",
                isError = isAddressError,
                supportingText = if (isAddressError) {
                    { Text("Address cannot be empty.") }
                } else null,
                singleLine = false,
                maxLines = 3,
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            CommonInputField(
                value = chefViewModel.postcode,
                onValueChange = { input ->
                    chefViewModel.postcode = input.filter { it.isDigit() }.take(5)
                },
                textId = R.string.postcode,
                placeholder = "Enter your Postcode",
                isError = isPostcodeError,
                supportingText = if (isPostcodeError) {
                    { Text("Postcode must contain exactly 5 digits.") }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            DropDownList(
                labelId = R.string.state,
                placeholderId = R.string.select_state,
                selectedValue = chefViewModel.state,
                options = States,
                onOptionSelected = { chefViewModel.updateState(it ?: "") }
            )
            if (isStateError) {
                Text(
                    text = "Please select a state.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Validate input
                    // Save data to ViewModel
                    if (chefViewModel.validateAddressInfo()) {
                        navController.navigate("descriptionInfo")
                    }
                },
                enabled = chefViewModel.canProceedAddressInfo(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }
        }

    }
}
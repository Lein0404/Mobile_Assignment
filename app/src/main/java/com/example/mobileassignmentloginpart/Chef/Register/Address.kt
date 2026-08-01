package com.example.mobileassignmentloginpart.Chef.Register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobileassignmentloginpart.Chef.ViewModel.chefRegisterViewModel
import com.example.mobileassignmentloginpart.R
import com.example.mobileassignmentloginpart.ui.components.CommonInputField
import com.example.mobileassignmentloginpart.ui.components.DropDownList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addressInfo(
    navController: NavController,
    chefviewModel: chefRegisterViewModel
) {

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
                value = chefviewModel.address,
                onValueChange = {
                    chefviewModel.address = it
                },
                textId = R.string.address,
                placeholder = "Enter your address",
                modifier = Modifier.fillMaxWidth()
            )
            if (
                chefviewModel.showAddressErrorMessage &&
                !chefviewModel.isValidAddress()
            ) {
                Text(
                    text = "Address cannot be empty.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            CommonInputField(
                value = chefviewModel.postcode,
                onValueChange = {
                    chefviewModel.postcode = it
                },
                textId = R.string.postcode,
                placeholder = "Enter your Postcode",
                modifier = Modifier.fillMaxWidth()
            )
            if (
                chefviewModel.showAddressErrorMessage &&
                !chefviewModel.isValidPostcode()
            ) {
                Text(
                    text = "Postcode must contain 5 digits.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            DropDownList(
                labelId = R.string.state,
                placeholderId = R.string.select_state,
                selectedValue = chefviewModel.state,
                options = listOf(
                    "Pulau Pinang",
                    "Kedah",
                    "Perak",
                    "Perlis",
                    "Selangor",
                    "Negeri Sembilan",
                    "Johor",
                    "Melaka",
                    "Pahang",
                    "Terengganu",
                    "Sabah",
                    "Sarawak"
                ),
                onOptionSelected = {
                    chefviewModel.updateState(it)
                }
            )
            if (
                chefviewModel.showAddressErrorMessage &&
                !chefviewModel.isValidState()
            ) {
                Text(
                    text = "Please select a state.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Validate input
                    // Save data to ViewModel
                    if (chefviewModel.validateAddressInfo()) {
                        navController.navigate("descriptionInfo")
                    }
                },
                enabled = chefviewModel.canProceedAddressInfo(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }
        }

    }
}
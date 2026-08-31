package com.example.foodieheal.Chef.Register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.example.foodieheal.Chef.StateResList
import com.example.foodieheal.Chef.getStateDbName
import com.example.foodieheal.Chef.getStateResId
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.CommonInputField
import com.example.foodieheal.ui.components.DropDownList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addressInfo(
    navController: NavController,
    chefViewModel: ChefRegisterViewModel
) {
    val addressError = chefViewModel.addressErrorRes?.let { stringResource(it) }
    val postcodeError = chefViewModel.postcodeErrorRes?.let { stringResource(it) }
    val stateError = chefViewModel.stateErrorRes?.let { stringResource(it) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_address_info)) },
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
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.step_3_of_5),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            CommonInputField(
                value = chefViewModel.address,
                onValueChange = { chefViewModel.address = it },
                textId = R.string.address,
                placeholder = stringResource(R.string.placeholder_enter_address),
                isError = addressError != null,
                supportingText = addressError?.let { msg -> { Text(msg) } },
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
                placeholder = stringResource(R.string.placeholder_enter_postcode),
                isError = postcodeError != null,
                supportingText = postcodeError?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            DropDownList(
                labelId = R.string.state,
                placeholderId = R.string.select_state,
                selectedValue = getStateResId(chefViewModel.state)?.let { stringResource(it) } ?: chefViewModel.state,
                options = StateResList,
                onOptionSelected = { resId -> chefViewModel.updateState(getStateDbName(resId)) }
            )
            if (stateError != null) {
                Text(
                    text = stateError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (chefViewModel.validateAddressInfo()) {
                        navController.navigate("descriptionInfo")
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
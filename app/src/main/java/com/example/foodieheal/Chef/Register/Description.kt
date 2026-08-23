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
import androidx.compose.ui.Alignment
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
fun descriptionInfo(
    navController: NavController,
    chefViewModel: ChefRegisterViewModel
) {

    val isExperienceError =
        chefViewModel.showDescriptionErrorMessage && !chefViewModel.isValidExperience()
    val isDescriptionError =
        chefViewModel.showDescriptionErrorMessage && !chefViewModel.isValidDescription()
    val wordCount = chefViewModel.description.trim()
        .split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
        .size

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
                    progress = { 0.8f },
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
                text = "Step 4 of 5",
                style = MaterialTheme.typography.titleMedium
            )

            CommonInputField(
                value = chefViewModel.experience,
                onValueChange = { input ->
                    chefViewModel.experience = input.filter { it.isDigit() }.take(2)
                },
                textId = R.string.experience,
                placeholder = "Enter your years of experience",
                isError = isExperienceError,
                supportingText = if (isExperienceError) {
                    { Text("Please enter a valid years of experience.") }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(modifier = Modifier.fillMaxWidth()) {
                    CommonInputField(
                        value = chefViewModel.description,
                        onValueChange = { input ->
                            val currentWords = input.trim()
                                .split("\\s+".toRegex())
                                .filter { it.isNotEmpty() }
                                .size

                            if (currentWords <= 300) {
                                chefViewModel.description = input
                            }
                        },
                        textId = R.string.description,
                        placeholder = stringResource(R.string.description),
                        isError = isDescriptionError,
                        supportingText = if (isDescriptionError) {
                            { Text("Description cannot be empty.") }
                        } else null,
                        singleLine = false,
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "$wordCount/300 words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    // update data to view model
                    // do validation
                    onClick = {
                        if (chefViewModel.validateDescriptionInfo()) {
                            navController.navigate("chefPicture")
                        }
                    },
                    enabled = chefViewModel.canProceedDescriptionInfo(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }
}
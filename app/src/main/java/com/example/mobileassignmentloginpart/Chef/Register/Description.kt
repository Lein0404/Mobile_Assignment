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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobileassignmentloginpart.Chef.ViewModel.chefRegisterViewModel
import com.example.mobileassignmentloginpart.R
import com.example.mobileassignmentloginpart.ui.components.CommonInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun descriptionInfo(
    navController: NavController,
    chefRegisterViewModel: chefRegisterViewModel
){

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
                    progress = { 1f },
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
                text ="Description",
                style = MaterialTheme.typography.titleMedium
            )

            CommonInputField(
                value = chefRegisterViewModel.experience,
                onValueChange = {
                    chefRegisterViewModel.experience = it
                },
                textId = R.string.experience,
                placeholder = "Enter your experience",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number)
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                CommonInputField(
                    value = chefRegisterViewModel.description,
                    onValueChange = { input ->

                        val wordCount = input.trim()
                            .split("\\s+".toRegex())
                            .filter { it.isNotEmpty() }
                            .size

                        if (wordCount <= 300) {
                            chefRegisterViewModel.description = input
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
                    text = "${chefRegisterViewModel.description.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size}/300 words",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Button(
                // update data to view model
                // do validation
                onClick = {
                    navController.navigate("reviewInfo")
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
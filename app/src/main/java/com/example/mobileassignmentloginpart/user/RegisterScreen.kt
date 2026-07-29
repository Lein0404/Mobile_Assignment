package com.example.mobileassignmentloginpart.View

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobileassignmentloginpart.ViewModel.AuthViewModel
import com.example.mobileassignmentloginpart.navigation.Screen

@Composable
fun RegisterScreen(navController: NavController){
    val viewModel : AuthViewModel = viewModel()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length in 6..20
    val passwordsMatch = password == confirmPassword
    val isFormValid = isEmailValid && isPasswordValid && passwordsMatch && !viewModel.isProcessing

    LaunchedEffect(viewModel.registerSuccess) {
        if (viewModel.registerSuccess) {
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("< Back")
            }
            Text(
                text = "Register",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedTextField(
            value = email,
            onValueChange = {email = it},
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            enabled = !viewModel.isProcessing,
            isError = email.isNotEmpty() && !isEmailValid,
            supportingText = {
                if (email.isNotEmpty() && !isEmailValid) {
                    Text("Invalid email format")
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {password = it},
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            enabled = !viewModel.isProcessing,
            isError = password.isNotEmpty() && !isPasswordValid,
            supportingText = {
                if (password.isNotEmpty() && !isPasswordValid) {
                    Text("Password must be 6-20 characters")
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {confirmPassword = it},
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            enabled = !viewModel.isProcessing,
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = {
                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    Text("Passwords do not match")
                }
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        
        if (viewModel.isProcessing) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.register(email, password) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                enabled = isFormValid,
                border = if (!isFormValid) BorderStroke(1.dp, Color.Gray) else null
            ) {
                Text("Submit")
            }
        }

        if (viewModel.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = viewModel.errorMessage, color = Color.Red)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

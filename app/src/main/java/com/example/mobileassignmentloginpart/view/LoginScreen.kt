package com.example.mobileassignmentloginpart.view

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
import com.example.mobileassignmentloginpart.viewModel.AuthViewModel
import com.example.mobileassignmentloginpart.navigation.Screen

@Composable
fun LoginScreen(navController: NavController){
    val viewModel: AuthViewModel = viewModel()
    var email by remember{ mutableStateOf("") }
    var password by remember{mutableStateOf("")}
    
    val isFormValid = email.isNotEmpty() && password.isNotEmpty() && !viewModel.isProcessing

    LaunchedEffect(viewModel.loginSuccess) {
        if(viewModel.loginSuccess){
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {email = it},
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            enabled = !viewModel.isProcessing
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {password = it},
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            enabled = !viewModel.isProcessing
        )

        Spacer(modifier = Modifier.height(20.dp))
        
        if (viewModel.isProcessing) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                enabled = isFormValid,
                border = if (!isFormValid) BorderStroke(1.dp, Color.Gray) else null
            ){
                Text("Login")
            }
            
            TextButton(
                onClick = { viewModel.forgotPassword(email) }
            ) {
                Text("Forgot Password?")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        TextButton(
            onClick = {
                navController.navigate(Screen.Register.route)
            }
        ) {
            Text("Don't have an account? Register")
        }

        if(viewModel.errorMessage.isNotEmpty()){
            Spacer(modifier = Modifier.height(10.dp))
            val isSuccess = viewModel.errorMessage.contains("sent")
            Text(
                text = viewModel.errorMessage, 
                color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
    }
}

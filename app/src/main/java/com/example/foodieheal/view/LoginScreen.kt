package com.example.foodieheal.view

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
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController){
    val viewModel: AuthViewModel = viewModel()
    var email by remember{ mutableStateOf("zh@gmail.com") }
    var password by remember{mutableStateOf("000000")}

    val isFormValid = email.isNotEmpty() && password.isNotEmpty() && !viewModel.isProcessing

    LaunchedEffect(viewModel.loginSuccess) {
        if(viewModel.loginSuccess){
            navController.navigate(Screen.Main.route) {
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
        Text(
            text = "Login", 
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black // Force black color
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {email = it},
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            enabled = !viewModel.isProcessing,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                focusedLabelColor = Color.Black,
                cursorColor = Color.Black
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {password = it},
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            enabled = !viewModel.isProcessing,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                focusedLabelColor = Color.Black,
                cursorColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (viewModel.isProcessing) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                border = if (!isFormValid) BorderStroke(1.dp, Color.Gray) else null
            ){
                Text("Login")
            }

            TextButton(
                onClick = { viewModel.forgotPassword(email) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Forgot Password?")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        TextButton(
            onClick = {
                navController.navigate(Screen.Register.route)
            },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
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

package com.example.foodieheal.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length in 8..20
    val passwordsMatch = password == confirmPassword
    val isFormValid = isEmailValid && isPasswordValid && passwordsMatch && !viewModel.isProcessing

    LaunchedEffect(viewModel.registerSuccess) {
        if (viewModel.registerSuccess) {
            // Navigate to Body Status screen after registration
            navController.navigate(com.example.foodieheal.navigation.Screen.EditBodyStatus.route + "?fromRegister=true") {
                popUpTo(com.example.foodieheal.navigation.Screen.Register.route) { inclusive = true }
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
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("< Back")
            }
            Text(
                text = "Register",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
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
            },
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
            isError = password.isNotEmpty() && !isPasswordValid,
            supportingText = {
                if (password.isNotEmpty() && !isPasswordValid) {
                    Text("Password must be 8-20 characters")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                focusedLabelColor = Color.Black,
                cursorColor = Color.Black
            )
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
            },
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
                onClick = { viewModel.register(email, password) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                border = if (!isFormValid) BorderStroke(1.dp, Color.Gray) else null
            ) {
                Text("Submit")
            }
        }

        if (viewModel.errorMessage.isNotEmpty()) {
            val isAlreadyRegistered = viewModel.errorMessage.contains("already registered", ignoreCase = true)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = if (isAlreadyRegistered) Color(0xFFE3F2FD) else Color(0xFFFFEBEE),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = if (isAlreadyRegistered) com.example.foodieheal.R.drawable.ic_help else com.example.foodieheal.R.drawable.cancel),
                        contentDescription = null,
                        tint = if (isAlreadyRegistered) Color(0xFF1976D2) else Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = viewModel.errorMessage,
                        color = if (isAlreadyRegistered) Color(0xFF1976D2) else Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

package com.example.foodieheal.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController){
    // 🌟 Share ViewModel with MainActivity
    val viewModel: AuthViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner)
    val chefRegisterViewModel: chefRegisterViewModel = viewModel()
    var email by remember{ mutableStateOf("") }
    var password by remember{mutableStateOf("")}
    
    val isFormValid = email.isNotEmpty() && password.isNotEmpty() && !viewModel.isProcessing

    LaunchedEffect(Unit) {
        chefRegisterViewModel.resetRegistrationFlow()
    }


    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            when {
                viewModel.isAdmin -> {
                    navController.navigate(Screen.AdminChefScreen.route) {
                        popUpTo(0) { // Clear the back stack mean back to first page <Login page>
                            inclusive = true
                        }
                        launchSingleTop = true //use to prevent create duplicate same screen
                    }
                }
                viewModel.isChef -> {
                    navController.navigate(Screen.ChefMain.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
                else -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                            launchSingleTop = true
                    }
                }
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
            color = Color.Black
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

        Spacer(modifier = Modifier.height(5.dp))
        TextButton(
            onClick = {
                navController.navigate(Screen.Welcome.route)
            }
        ) {
            Text("Register as a Chef")
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

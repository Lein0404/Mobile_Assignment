package com.example.mobileassignmentloginpart.View

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobileassignmentloginpart.ViewModel.AuthViewModel
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
        TextField(
            value = email,
            onValueChange = {email = it},
            label = {
                Text("Email")
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        TextField(
            value = password,
            onValueChange = {password = it},
            label = {
                Text("Password")
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        
        if (viewModel.isProcessing) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.login(
                        email,
                        password
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                enabled = isFormValid,
                border = if (!isFormValid) BorderStroke(1.dp, Color.Gray) else null
            ){
                Text("Login")
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
            Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

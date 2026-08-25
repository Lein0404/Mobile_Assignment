package com.example.foodieheal.User.View

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterViewModel
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.User.viewModel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val chefRegisterViewModel: ChefRegisterViewModel = viewModel()
    
    // 🌟 Track if user has tried to submit for validation display
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // 🌟 EXTRA Strict Email Validation (Requires at least 3 chars for TLD like .com)
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{3,}$".toRegex()
    val isEmailFormatValid = email.matches(emailRegex)
    val isFormValid = email.isNotEmpty() && password.isNotEmpty() && !viewModel.isProcessing

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // Update Status Bar Color
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    LaunchedEffect(Unit) {
        chefRegisterViewModel.resetRegistrationFlow()
    }


    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            when {
                viewModel.isAdmin -> {
                    navController.navigate(Screen.AdminChefScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                viewModel.isChef -> {
                    navController.navigate(Screen.ChefMain.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                else -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 🌟 Seamless Orange Status Bar Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)
                .statusBarsPadding()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.6f))

            Text(
                text = "Login",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

        Spacer(modifier = Modifier.height(48.dp))

        // Email Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Email",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = email,
                onValueChange = { 
                    email = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false 
                    if (viewModel.errorMessage.isNotEmpty()) viewModel.resetPasswordState()
                },
                placeholder = { Text("Email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            // 🌟 Client-side Format Validation ONLY
            // This now catches .co or .c because we require 3+ chars for the domain
            if (hasAttemptedSubmit && !isEmailFormatValid && email.isNotEmpty()) {
                Text(
                    text = "Invalid email",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Password Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = password,
                onValueChange = { 
                    password = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                    // 🌟 Clear server errors when user starts fixing the input
                    if (viewModel.errorMessage.isNotEmpty()) viewModel.resetPasswordState()
                },
                placeholder = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) R.drawable.ic_view else R.drawable.ic_hide
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(painter = painterResource(id = image), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            
            // 🌟 Simplified Server Error
            if (viewModel.errorMessage.isNotEmpty() && viewModel.errorMessage.contains("Invalid login credentials", ignoreCase = true)) {
                Text(
                    text = "Invalid password",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Forgot Password
        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(
                text = "Forget Password?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { viewModel.forgotPassword(email) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login Button
        Button(
            onClick = { 
                if (isEmailFormatValid) {
                    viewModel.login(email, password)
                } else {
                    hasAttemptedSubmit = true
                }
            },
            modifier = Modifier
                .width(150.dp)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            enabled = isFormValid && !viewModel.isProcessing
        ) {
            if (viewModel.isProcessing) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("LOGIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Footer Link
        Text(
            text = "Don't have an account? Sign up here!",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { 
                navController.navigate(Screen.Register.route)
            }
        )
        
        // Register as Chef option
        TextButton(
            onClick = { navController.navigate(Screen.Welcome.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Register as a Chef", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }

        // Generic Error Message (if not wrong password)
        if (viewModel.errorMessage.isNotEmpty() && !viewModel.errorMessage.contains("Invalid login credentials", ignoreCase = true)) {
            val isSuccess = viewModel.errorMessage.contains("sent")
            Text(
                text = viewModel.errorMessage,
                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        }
    }
}

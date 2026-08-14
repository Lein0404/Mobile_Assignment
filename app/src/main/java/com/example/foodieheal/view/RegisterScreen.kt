package com.example.foodieheal.view

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
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // 🌟 Track if user has tried to submit
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // 🌟 Strict Validation Logic
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{3,}$".toRegex()
    val isEmailValid = email.matches(emailRegex)
    val isPasswordValid = password.length in 8..20
    val passwordsMatch = password == confirmPassword
    val isFormValid = email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && !viewModel.isProcessing

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // Sync Status Bar Color
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(painterResource(id = R.drawable.ic_arrowback), "Back", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Register",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Email", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
            TextField(
                value = email,
                onValueChange = { 
                    email = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false 
                },
                placeholder = { Text("Email", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = Color(0xFFE8E8E8),
                    focusedContainerColor = Color(0xFFE8E8E8),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            if (hasAttemptedSubmit && !isEmailValid && email.isNotEmpty()) {
                Text(text = "Invalid email", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Password Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
            TextField(
                value = password,
                onValueChange = { 
                    password = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                },
                placeholder = { Text("Password", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) R.drawable.ic_view else R.drawable.ic_hide
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(painter = painterResource(id = image), null, modifier = Modifier.size(20.dp))
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = Color(0xFFE8E8E8),
                    focusedContainerColor = Color(0xFFE8E8E8),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            if (hasAttemptedSubmit && !isPasswordValid && password.isNotEmpty()) {
                Text(text = "Password must be 8-20 characters", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Confirm Password Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Confirm Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
            TextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                },
                placeholder = { Text("Confirm Password", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) R.drawable.ic_view else R.drawable.ic_hide
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(painter = painterResource(id = image), null, modifier = Modifier.size(20.dp))
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = Color(0xFFE8E8E8),
                    focusedContainerColor = Color(0xFFE8E8E8),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            if (hasAttemptedSubmit && !passwordsMatch && confirmPassword.isNotEmpty()) {
                Text(text = "Passwords do not match", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Register Button
        Button(
            onClick = { 
                if (isEmailValid && isPasswordValid && passwordsMatch) {
                    // 🌟 Unified Logic: Set credentials and move to next step
                    viewModel.setTempCredentials(email, password)
                    navController.navigate(com.example.foodieheal.navigation.Screen.EditBodyStatus.route + "?fromRegister=true")
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
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFF0F0F0),
                disabledContentColor = Color(0xFF666666)
            ),
            enabled = isFormValid && !viewModel.isProcessing
        ) {
            if (viewModel.isProcessing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("NEXT", fontWeight = FontWeight.Bold, fontSize = 16.sp) // 🌟 Changed to "NEXT"
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Footer
        Text(
            text = "Already have an account? Login here!",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { navController.popBackStack() }
        )

        if (viewModel.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = viewModel.errorMessage, color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

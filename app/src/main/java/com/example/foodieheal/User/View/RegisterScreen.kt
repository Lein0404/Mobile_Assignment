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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.example.foodieheal.User.viewModel.AuthViewModel

@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Track if user has tried to submit
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // Strict Validation Logic
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{3,}$".toRegex()
    val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$".toRegex()
    val isEmailValid = email.matches(emailRegex)
    val isPasswordValid = password.matches(passwordRegex)
    val passwordsMatch = password == confirmPassword
    val isFormValid = email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && !viewModel.isProcessing

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // Resource strings for robust error comparison
    val emailAlreadyRegistered = stringResource(R.string.error_email_already_registered)

    LaunchedEffect(Unit) {
        viewModel.resetPasswordState() // Clear errors when re-entering Register
    }

    // Sync Status Bar Color
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Seamless Orange Status Bar Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor)
                    .statusBarsPadding()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back Button
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = {
                        // Prevent splash screen and crashes by ensuring we only pop if we are on Register
                        if (navController.currentDestination?.route == Screen.Register.route) {
                            navController.popBackStack(Screen.Login.route, inclusive = false)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-12).dp)
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_arrowback),
                        stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User icon
            Icon(
                painter = painterResource(id = R.drawable.login_register),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Text(
                text = stringResource(R.string.register_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(R.string.register_subtitle),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = androidx.compose.ui.text.TextStyle(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.email_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (hasAttemptedSubmit) hasAttemptedSubmit = false
                        if (viewModel.errorMessage.isNotEmpty()) viewModel.resetPasswordState()
                    },
                    placeholder = { Text(stringResource(R.string.email_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Email Specific Errors (Local Validation + Server Check)
                val emailError = when {
                    hasAttemptedSubmit && !isEmailValid && email.isNotEmpty() -> stringResource(R.string.error_invalid_email)
                    viewModel.errorMessage == emailAlreadyRegistered ||
                    viewModel.errorMessage.contains("already registered", ignoreCase = true) || 
                    viewModel.errorMessage.contains("already exists", ignoreCase = true) -> emailAlreadyRegistered
                    else -> null
                }

                if (emailError != null) {
                    Text(
                        text = emailError,
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
                    text = stringResource(R.string.password_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (it.length <= 20) {
                            password = it
                            if (hasAttemptedSubmit) hasAttemptedSubmit = false
                        }
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.password_label),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) R.drawable.ic_view else R.drawable.ic_hide
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(id = image),
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text("${password.length}/20", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (hasAttemptedSubmit && !isPasswordValid && password.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.error_password_requirement),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Password Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.confirm_password_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        if (it.length <= 20) {
                            confirmPassword = it
                            if (hasAttemptedSubmit) hasAttemptedSubmit = false
                        }
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.confirm_password_label),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (confirmPasswordVisible) R.drawable.ic_view else R.drawable.ic_hide
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                painter = painterResource(id = image),
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text("${confirmPassword.length}/20", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (hasAttemptedSubmit && !passwordsMatch && confirmPassword.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.error_passwords_not_matching),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Register Button
            Button(
                onClick = {
                    if (isEmailValid && isPasswordValid && passwordsMatch) {
                        // Validate email uniqueness before moving to next screen
                        viewModel.validateEmailUniqueness(email) {
                            viewModel.setTempCredentials(email, password)
                            navController.navigate(Screen.EditBodyStatus.route + "?fromRegister=true")
                        }
                    } else {
                        hasAttemptedSubmit = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
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
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(stringResource(R.string.next_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = stringResource(R.string.login_prompt),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    if (navController.currentDestination?.route == Screen.Register.route) {
                        navController.popBackStack(Screen.Login.route, inclusive = false)
                    }
                }
            )

            // Only show non-auth errors here (e.g. Network errors)
            // We filtered out "already registered" and auth-related messages since they are shown under fields
            val isAuthError = viewModel.errorMessage == emailAlreadyRegistered ||
                             viewModel.errorMessage.contains("already registered", ignoreCase = true) ||
                             viewModel.errorMessage.contains("Invalid email or password", ignoreCase = true) ||
                             viewModel.errorMessage.contains("Invalid login credentials", ignoreCase = true) ||
                             viewModel.errorMessage.contains("Registration Failed", ignoreCase = true)

            if (viewModel.errorMessage.isNotEmpty() && !isAuthError) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

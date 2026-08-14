package com.example.foodieheal.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner)
    
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // 🌟 Track if user has tried to submit
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // 🌟 Validation Logic
    val isLengthValid = newPassword.length in 8..20
    val passwordsMatch = newPassword == confirmPassword
    
    // Button is enabled if all fields are filled
    val isFormFilled = oldPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank()

    // 🌟 Simplified: No more collection logic here, avoids the "kick back" bug entirely
    DisposableEffect(Unit) {
        authViewModel.clearProfileEvents() // Clear any old success messages when entering
        onDispose { }
    }

    Scaffold(
        modifier = Modifier.imePadding(), 
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
                .verticalScroll(rememberScrollState()) 
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Secure Your Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Enter your current and new password below to update.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Old Password
            PasswordInputField(
                label = "Current Password",
                value = oldPassword,
                onValueChange = { 
                    oldPassword = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                    if (authViewModel.passwordErrorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                },
                // 🌟 FIX: Only looks at password-specific errors now
                isError = authViewModel.passwordErrorMessage.isNotEmpty(),
                supportingText = if (authViewModel.passwordErrorMessage.isNotEmpty()) authViewModel.passwordErrorMessage else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // New Password
            PasswordInputField(
                label = "New Password",
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                    if (authViewModel.errorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                },
                // 🌟 FIX: Wait for server result so it pops up at the exact same time as the current password error
                isError = hasAttemptedSubmit && !authViewModel.isProcessing && !isLengthValid,
                supportingText = if (hasAttemptedSubmit && !authViewModel.isProcessing && !isLengthValid) "Password must be 8-20 characters" else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            PasswordInputField(
                label = "Confirm New Password",
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                    if (authViewModel.errorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                },
                // 🌟 FIX: Wait for server result so it pops up at the exact same time as the others
                isError = hasAttemptedSubmit && !authViewModel.isProcessing && !passwordsMatch,
                supportingText = if (hasAttemptedSubmit && !authViewModel.isProcessing && !passwordsMatch) "Passwords do not match" else null
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    hasAttemptedSubmit = true 
                    // 🌟 Use the callback to navigate ONLY when the server confirms success
                    authViewModel.changePassword(oldPassword, newPassword) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color(0xFFD1D1D1),
                    disabledContentColor = Color(0xFF666666)
                ),
                enabled = isFormFilled && !authViewModel.isProcessing
            ) {
                if (authViewModel.isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("UPDATE PASSWORD", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PasswordInputField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // 🌟 Removed the tight height constraint so the text doesn't look squashed
            modifier = Modifier.fillMaxWidth(), 
            singleLine = true,
            isError = isError,
            // 🌟 Increased font size for better readability
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp), 
            supportingText = {
                if (supportingText != null) {
                    Text(text = supportingText, color = Color.Red, fontSize = 12.sp)
                }
            },
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                val image = if (passwordVisible) R.drawable.ic_view else R.drawable.ic_hide
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(painter = painterResource(id = image), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                unfocusedContainerColor = Color(0xFFE8E8E8),
                focusedContainerColor = Color(0xFFE8E8E8),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                // 🌟 Keep background normal even if there is an error
                errorContainerColor = Color(0xFFE8E8E8),
                errorIndicatorColor = Color.Transparent,
                errorTextColor = Color.Black,
                errorCursorColor = Color.Black
            )
        )
    }
}

package com.example.foodieheal.User.View


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.R
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.meal_planner.screen.OfflinePlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Track if user has tried to submit
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // Validation Logic
    val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S{8,20}$".toRegex()
    val isPasswordValid = newPassword.matches(passwordRegex)
    val passwordsMatch = newPassword == confirmPassword
    val isSameAsOld = newPassword.trim() == oldPassword.trim() && newPassword.isNotEmpty()
    
    // Button is enabled if all fields are filled
    val isFormFilled = oldPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank()

    // Simplified: No more collection logic here, avoids the "kick back" bug entirely
    DisposableEffect(Unit) {
        authViewModel.clearProfileEvents() // Clear any old success messages when entering
        onDispose { }
    }

    Scaffold(
        modifier = Modifier.imePadding(), 
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_change_password), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Safety check to prevent spam-clicks from causing navigation crashes or "blank screens"
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute?.contains(Screen.ChangePassword.route) == true) {
                            navController.popBackStack() 
                        }
                    }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), stringResource(R.string.back_button), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (!authViewModel.isNetworkAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                OfflinePlaceholder(message = stringResource(R.string.desc_connect_internet_password))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState()) 
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(
                text = stringResource(R.string.change_password_secure_account),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = stringResource(R.string.change_password_enter_passwords),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Old Password
            PasswordInputField(
                label = stringResource(R.string.current_password),
                value = oldPassword,
                onValueChange = { 
                    if (it.length <= 20) {
                        oldPassword = it
                        if (hasAttemptedSubmit) hasAttemptedSubmit = false
                        if (authViewModel.passwordErrorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                    }
                },
                // Only looks at password-specific errors now
                isError = authViewModel.passwordErrorMessage.isNotEmpty(),
                supportingText = if (authViewModel.passwordErrorMessage.isNotEmpty()) authViewModel.passwordErrorMessage else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // New Password
            PasswordInputField(
                label = stringResource(R.string.new_password),
                value = newPassword,
                onValueChange = { 
                    if (it.length <= 20) {
                        newPassword = it
                        if (hasAttemptedSubmit) hasAttemptedSubmit = false
                        if (authViewModel.errorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                    }
                },
                // Wait for server result so it pops up at the exact same time as the current password error
                isError = hasAttemptedSubmit && !authViewModel.isProcessing && (!isPasswordValid || isSameAsOld),
                supportingText = if (hasAttemptedSubmit && !authViewModel.isProcessing) {
                    when {
                        newPassword.contains(" ") -> stringResource(R.string.error_password_no_spaces)
                        !isPasswordValid -> stringResource(R.string.change_password_validation_error)
                        isSameAsOld -> stringResource(R.string.error_password_same_as_old)
                        else -> null
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            PasswordInputField(
                label = stringResource(R.string.confirm_new_password),
                value = confirmPassword,
                onValueChange = { 
                    if (it.length <= 20) {
                        confirmPassword = it
                        if (hasAttemptedSubmit) hasAttemptedSubmit = false
                        if (authViewModel.errorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                    }
                },
                // Wait for server result so it pops up at the exact same time as the others
                isError = hasAttemptedSubmit && !authViewModel.isProcessing && !passwordsMatch,
                supportingText = if (hasAttemptedSubmit && !authViewModel.isProcessing && !passwordsMatch) stringResource(R.string.change_password_match_error) else null
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    hasAttemptedSubmit = true 
                    val cleanOldPassword = oldPassword.trim()
                    val cleanNewPassword = newPassword.trim()
                    if (isPasswordValid && passwordsMatch && !isSameAsOld) {
                        // Use the callback to navigate ONLY when the server confirms success
                        authViewModel.changePassword(cleanOldPassword, cleanNewPassword) {
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                enabled = isFormFilled && !authViewModel.isProcessing
            ) {
                if (authViewModel.isProcessing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.btn_update_password), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
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
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // Removed the tight height constraint so the text doesn't look squashed
            modifier = Modifier.fillMaxWidth(), 
            singleLine = true,
            isError = isError,
            // Increased font size for better readability
            textStyle = TextStyle(fontSize = 16.sp),
            supportingText = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (supportingText != null) {
                            Text(text = supportingText, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Text("${value.length}/20", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                val image = if (passwordVisible) R.drawable.ic_view else R.drawable.ic_hide
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(painter = painterResource(id = image), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                // Keep background normal even if there is an error
                errorContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
                errorCursorColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

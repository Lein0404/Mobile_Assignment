package com.example.foodieheal.Chef.Home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterValidate
import com.example.foodieheal.R
import com.example.foodieheal.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefChangePasswordScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val passwordUpdateSuccessMessage = stringResource(R.string.toast_password_updated_success)

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // Validation using ChefRegisterValidate
    val isNewPasswordValid = ChefRegisterValidate.isValidPassword(newPassword)
    val passwordsMatch = ChefRegisterValidate.isPasswordMatched(newPassword, confirmPassword)
    val isFormFilled = oldPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank()

    DisposableEffect(Unit) {
        authViewModel.clearProfileEvents()
        authViewModel.resetPasswordState()
        onDispose { }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.change_password),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
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
                text = stringResource(R.string.secure_account_chef),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = stringResource(R.string.change_password_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Old Password
            ChefPasswordInputField(
                label = stringResource(R.string.current_password),
                value = oldPassword,
                onValueChange = {
                    oldPassword = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                    if (authViewModel.passwordErrorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                },
                isError = authViewModel.passwordErrorMessage.isNotEmpty(),
                supportingText = if (authViewModel.passwordErrorMessage.isNotEmpty()) authViewModel.passwordErrorMessage else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // New Password
            ChefPasswordInputField(
                label = stringResource(R.string.new_password),
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                    if (authViewModel.errorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                },
                isError = hasAttemptedSubmit && !authViewModel.isProcessing && !isNewPasswordValid,
                supportingText = if (hasAttemptedSubmit && !authViewModel.isProcessing && !isNewPasswordValid) {
                    stringResource(R.string.error_chef_password_rule)
                } else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            ChefPasswordInputField(
                label = stringResource(R.string.confirm_new_password),
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (hasAttemptedSubmit) hasAttemptedSubmit = false
                    if (authViewModel.errorMessage.isNotEmpty()) authViewModel.resetPasswordState()
                },
                isError = hasAttemptedSubmit && !authViewModel.isProcessing && !passwordsMatch,
                supportingText = if (hasAttemptedSubmit && !authViewModel.isProcessing && !passwordsMatch) {
                    stringResource(R.string.error_passwords_not_matching)
                } else null
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (isNewPasswordValid && passwordsMatch) {
                        authViewModel.changePassword(oldPassword, newPassword) {
                            Toast.makeText(
                                context,
                                passwordUpdateSuccessMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color(0xFFD1D1D1),
                    disabledContentColor = Color(0xFF666666)
                ),
                enabled = isFormFilled && !authViewModel.isProcessing
            ) {
                if (authViewModel.isProcessing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.btn_update_password),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChefPasswordInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
            supportingText = {
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                val image = if (passwordVisible) R.drawable.ic_view else R.drawable.ic_hide
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(id = image),
                        contentDescription = if (passwordVisible) {
                            stringResource(R.string.hide_password)
                        } else {
                            stringResource(R.string.show_password)
                        },
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                errorIndicatorColor = Color.Transparent,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
                errorCursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

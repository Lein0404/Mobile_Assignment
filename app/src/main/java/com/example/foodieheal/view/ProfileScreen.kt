package com.example.foodieheal.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.foodieheal.navigation.*
import java.io.ByteArrayOutputStream

@Composable
fun ProfileScreen(navController: NavController) {
    val viewModel: AuthViewModel = viewModel()
    val user = viewModel.currentUser
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var profilePicBase64 by remember { mutableStateOf("") }

    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.isEmpty() || password.length >= 6
    val passwordsMatch = password == confirmPassword
    val isFormValid = isEmailValid && isPasswordValid && passwordsMatch && !viewModel.isProcessing

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            profilePicBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
        }
    }

    LaunchedEffect(user) {
        user?.let {
            name = it.name ?: ""
            email = it.email ?: ""
            profilePicBase64 = it.profilePicUrl ?: ""
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        if (viewModel.errorMessage == "Profile Updated") {
            password = ""
            confirmPassword = ""
        }
    }

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text("< Back")
                    }
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = !viewModel.isProcessing) { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(profilePicBase64) {
                        if (profilePicBase64.isNotEmpty()) {
                            try {
                                val imageBytes = Base64.decode(profilePicBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = if (name.isNotEmpty()) name.take(1).uppercase() else "?",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            item { Text("Tap to change picture", fontSize = 10.sp, color = Color.Gray) }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { Text(text = "User ID: ${user.customId ?: "U001"}", fontWeight = FontWeight.Bold) }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isProcessing
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isProcessing
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("New Password (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isProcessing,
                    isError = password.isNotEmpty() && !isPasswordValid,
                    supportingText = {
                        if (password.isNotEmpty() && !isPasswordValid) {
                            Text("Password must be at least 6 characters")
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isProcessing,
                    isError = password.isNotEmpty() && !passwordsMatch,
                    supportingText = {
                        if (password.isNotEmpty() && !passwordsMatch) {
                            Text("Passwords do not match")
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                if (viewModel.isProcessing) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            viewModel.updateProfile(name, email, profilePicBase64)
                            if (password.isNotEmpty()) {
                                viewModel.updatePassword(password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isFormValid,
                        border = if (!isFormValid) BorderStroke(1.dp, Color.Gray) else null
                    ) {
                        Text("Update Profile")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Button(
                    onClick = {
                        viewModel.logout {
                            navController.navigate(Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !viewModel.isProcessing,
                    border = if (viewModel.isProcessing) BorderStroke(1.dp, Color.Gray) else null
                ) {
                    Text("Logout")
                }
            }

            item {
                if (viewModel.errorMessage.isNotEmpty()) {
                    val isSuccess = viewModel.errorMessage == "Profile Updated"
                    Text(
                        text = viewModel.errorMessage, 
                        color = if (isSuccess) Color(0xFF4CAF50) else Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

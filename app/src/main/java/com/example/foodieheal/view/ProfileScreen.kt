package com.example.foodieheal.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    parentNavController: NavController
) {
    val viewModel: AuthViewModel = viewModel()
    val user = viewModel.currentUser
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var profilePicBase64 by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F8F8))) {
        // 1. Header
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 8.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showBottomSheet = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_hamburger_menu),
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Profile",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

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
                item { Text(text = "User ID: ${user.customId ?: "U001"}", fontWeight = FontWeight.Bold, color = Color.Black) }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
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
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
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
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
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
                                parentNavController.navigate(Screen.Login.route) {
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

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                BottomSheetMenuItem(
                    icon = R.drawable.ic_square_edit,
                    label = "Edit Profile",
                    onClick = { /* Action empty */ }
                )
                BottomSheetMenuItem(
                    icon = R.drawable.ic_edit_body_status,
                    label = "Edit Body Status",
                    onClick = { /* Action empty */ }
                )
                BottomSheetMenuItem(
                    icon = R.drawable.ic_ingredient_list,
                    label = "View & Request Ingredients",
                    onClick = {
                        showBottomSheet = false
                        navController.navigate(Screen.Ingredients.route)
                    }
                )
                BottomSheetMenuItem(
                    icon = R.drawable.ic_shopping_cart,
                    label = "Shopping Cart",
                    onClick = { /* Action empty */ }
                )
                BottomSheetMenuItem(
                    icon = R.drawable.ic_register_as_chef,
                    label = "Register as Chef",
                    onClick = { /* Action empty */ }
                )
                BottomSheetMenuItem(
                    icon = R.drawable.ic_history,
                    label = "Appointment History",
                    onClick = { /* Action empty */ }
                )
            }
        }
    }
}

@Composable
fun BottomSheetMenuItem(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
    }
}

package com.example.foodieheal.User.View

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.navigation.Screen
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.meal_planner.screen.OfflinePlaceholder
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val user = authViewModel.currentUser
    val context = LocalContext.current

    var name by remember { mutableStateOf(user?.name ?: "") }
    var description by remember { mutableStateOf(user?.description ?: "") }
    var profilePicUrl by remember { mutableStateOf(user?.profilePicUrl ?: "") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            imageBytes = outputStream.toByteArray()
            selectedBitmap = scaledBitmap
        }
    }

    DisposableEffect(Unit) {
        authViewModel.clearProfileEvents()
        onDispose { }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Safety check to prevent spam-clicks from causing navigation crashes or "blank screens"
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute?.contains(Screen.EditProfile.route) == true) {
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
                OfflinePlaceholder(message = stringResource(R.string.desc_connect_internet_profile))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(stringResource(R.string.edit_profile_pic), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!profilePicUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = profilePicUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = name.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { launcher.launch("image/*") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.edit_profile_select_image), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.edit_profile_username), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    placeholder = { Text(stringResource(R.string.edit_profile_username), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text("${name.length}/20", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.description), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        val newlineCount = it.count { char -> char == '\n' }
                        if (it.length <= 150 && newlineCount <= 4) {
                            description = it 
                        }
                    },
                    placeholder = { Text(stringResource(R.string.edit_profile_desc_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val newlineCount = description.count { char -> char == '\n' }
                            Text("${newlineCount + 1}/5 lines", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${description.length}/150", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (authViewModel.isProcessing) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        authViewModel.updateProfile(
                            name = name, 
                            email = user?.email ?: "", 
                            profilePicUrl = profilePicUrl, 
                            description = description,
                            imageBytes = imageBytes
                        ) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.edit_profile_save_changes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
}

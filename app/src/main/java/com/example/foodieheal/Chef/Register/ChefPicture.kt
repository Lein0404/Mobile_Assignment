package com.example.foodieheal.Chef.Register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel
import com.example.foodieheal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefPictureScreen(
    navController: NavController,
    chefViewModel: chefRegisterViewModel
) {

    val isImageError = chefViewModel.showDescriptionErrorMessage && !chefViewModel.isValidProfilePicture()

    //Photo picker launching
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // Update the selected image URI in the ViewModel
        if (uri != null) {
            chefViewModel.updateImage(uri)
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text("Profile Picture")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrowback),
                                contentDescription = "Back"
                            )
                        }
                    }
                )
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Step 5 of 5",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Please upload a clear profile picture so clients can recognize you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (chefViewModel.selectedImageUri != null) {
                AsyncImage(
                    model = chefViewModel.selectedImageUri,
                    contentDescription = "Chef Profile Picture",
                    contentScale = ContentScale.Crop, // Fits avatar bounds cleanly
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .size(180.dp)
                        .border(
                            width = 2.dp,
                            color = if (isImageError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.camera_placeholder),
                                contentDescription = "No photo selected",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isImageError) {
                Text(
                    text = "A profile picture is required to complete registration.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Trigger photo picker after click
            OutlinedButton(
                onClick = {
                    launcher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (chefViewModel.selectedImageUri == null) "Choose Photo" else "Change Photo")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    chefViewModel.validateProfilePicture()
                    if (chefViewModel.canProceedReviewPage()) {
                        navController.navigate("reviewInfo")
                    }
                },
                enabled = chefViewModel.canProceedReviewPage(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }
        }
    }
}
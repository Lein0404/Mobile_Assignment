package com.example.foodieheal.Cloudinary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.SecondaryButton

/**
 * A reusable composable for selecting and previewing an image for Cloudinary upload.
 */
@Composable
fun CloudinaryUploadScreen(
    viewModel: CloudinaryUploadViewModel
) {
    val state = viewModel.uiState.collectAsState().value

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.setSelectedImage(uri) // just save selected URI
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.image),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Show selected local image, or fallback to uploaded remote URL
        val imageModel: Any? = state.selectedImageUri ?: state.uploadedImageUrl.ifEmpty { null }

        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = if (state.selectedImageUri != null) "Selected Image" else "Uploaded Image",
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Gray, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Show image error if any
        state.imageError?.let { errorResId ->
            Text(
                text = stringResource(errorResId),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        SecondaryButton(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth(),
            textId = if (state.selectedImageUri == null && state.uploadedImageUrl.isEmpty()) R.string.select_image else R.string.change_image
        )
    }
}
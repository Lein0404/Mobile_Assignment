package com.example.foodieheal.Cloudinary

import android.net.Uri

data class CloudinaryUploadUiState(
    val selectedImageUri: Uri? = null,
    val uploadedImageUrl: String = "",
    val imageError: Int? = null,
)
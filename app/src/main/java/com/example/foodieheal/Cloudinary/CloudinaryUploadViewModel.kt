package com.example.foodieheal.Cloudinary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.foodieheal.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CloudinaryUploadViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CloudinaryUploadUiState())
    val uiState: StateFlow<CloudinaryUploadUiState> = _uiState.asStateFlow()

    fun setSelectedImage(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri, imageError = null) }
    }

    private fun setUploadedImageUrl(url: String) {
        _uiState.update { it.copy(uploadedImageUrl = url, imageError = null) }
    }

    fun hasImage(): Boolean {
        return _uiState.value.selectedImageUri != null ||
                _uiState.value.uploadedImageUrl.isNotEmpty()
    }

    /**
     * Uploads the currently selected image to Cloudinary.
     * Returns the uploaded URL on success, or null if no image is selected.
     *
     * Call this from the parent ViewModel's submit flow, e.g.:
     *   val imageUrl = cloudinaryUploadViewModel.uploadImage(context)
     */
    suspend fun uploadImage(context: Context): String? {
        val uri = _uiState.value.selectedImageUri ?: return null
        return try {
            val url = context.uploadImageToCloudinary(uri)
            setUploadedImageUrl(url)
            // Clear the local URI since we now have a remote URL
            _uiState.update { it.copy(selectedImageUri = null) }
            url
        } catch (e: Exception) {
            _uiState.update { it.copy(imageError = R.string.upload_failed) }
            null
        }
    }

    /**
     * Pre-fill with an existing remote image URL (e.g. when editing a profile).
     */
    fun setExistingImageUrl(url: String) {
        _uiState.update { it.copy(uploadedImageUrl = url) }
    }

    fun clearState() {
        _uiState.value = CloudinaryUploadUiState()
    }
}
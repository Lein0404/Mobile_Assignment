package com.example.foodieheal.Cloudinary

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

suspend fun Context.uploadImageToCloudinary(
    uri: Uri,
    cloudName: String = CloudinaryConfig.CLOUD_NAME,
    uploadPreset: String = CloudinaryConfig.UPLOAD_PRESET,
    okHttpClient: OkHttpClient = OkHttpClient()
): String = withContext(Dispatchers.IO) {

    //Create a cache temp file
    val tempFile = File.createTempFile("upload_cache_", ".jpg", cacheDir).apply {
        deleteOnExit()
    }

    //Copy content/selected image to temp file
    try {
        contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Unable to open input stream for Uri: $uri")

        val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

        // Build up the request
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", uploadPreset)
            .addFormDataPart(
                "file",
                tempFile.name,
                tempFile.asRequestBody("image/*".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .build()

        // This is calling for request to cloudinary
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("Cloudinary upload failed (HTTP ${response.code}): ${errorBody ?: response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Cloudinary returned an empty response body")

            val json = JSONObject(responseBody)
            return@withContext json.getString("secure_url")
        }
        // This one finally done upload then clean the cache lo
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
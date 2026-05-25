package com.fixit.app.data.upload

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val api: UploadApi,
    @ApplicationContext private val context: Context,
) {
    /**
     * Reads the image at [uri] via the ContentResolver, uploads it to
     * POST /uploads/image, and returns the absolute public URL that can be
     * saved into any *_url column on the backend.
     */
    suspend fun uploadImage(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: throw IOException("Could not open image from device")

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val filename = when {
            mimeType.contains("png")  -> "upload.png"
            mimeType.contains("webp") -> "upload.webp"
            mimeType.contains("gif")  -> "upload.gif"
            else                      -> "upload.jpg"
        }

        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", filename, requestBody)
        return api.uploadImage(part).url
    }
}
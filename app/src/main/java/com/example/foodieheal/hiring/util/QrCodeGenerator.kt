package com.example.foodieheal.hiring.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.security.MessageDigest
import java.util.EnumMap

/**
 * Parsed payload data from a FoodieHeal appointment verification QR code.
 */
data class QrVerificationData(
    val appointmentId: String,
    val chefId: String,
    val userId: String,
    val date: String,
    val checksum: String
)

/**
 * Production-grade QR Code Generator and Cryptographic Payload Verifier.
 */
object QrCodeGenerator {

    private const val PAYLOAD_PREFIX = "FOODIEHEAL_VERIFY_V1"
    private const val SECRET_SALT = "FOODIEHEAL_QR_SALT_2026"

    /**
     * Builds a tamper-resistant verification payload string.
     */
    fun buildVerificationPayload(
        appointmentId: String,
        chefId: String,
        userId: String,
        date: String
    ): String {
        val rawData = "$appointmentId|$chefId|$userId|$date|$SECRET_SALT"
        val checksum = sha256(rawData).take(8)
        return "$PAYLOAD_PREFIX:$appointmentId:$chefId:$userId:$date:$checksum"
    }

    /**
     * Parses and validates a scanned QR code payload.
     * Returns the verification data if the payload structure and cryptographic checksum match.
     */
    fun parseVerificationPayload(rawPayload: String): QrVerificationData? {
        if (!rawPayload.startsWith(PAYLOAD_PREFIX)) return null
        val parts = rawPayload.split(":")
        if (parts.size != 6) return null

        val apptId = parts[1]
        val chefId = parts[2]
        val userId = parts[3]
        val date = parts[4]
        val checksum = parts[5]

        // Validate checksum
        val expectedChecksum = sha256("$apptId|$chefId|$userId|$date|$SECRET_SALT").take(8)
        if (checksum != expectedChecksum) return null

        return QrVerificationData(
            appointmentId = apptId,
            chefId = chefId,
            userId = userId,
            date = date,
            checksum = checksum
        )
    }

    /**
     * Generates an in-memory Android [Bitmap] containing the QR code matrix using ZXing.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H) // High error correction
                put(EncodeHintType.MARGIN, 1) // 1 module margin
            }

            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

package com.example.foodieheal.wallet.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.foodieheal.wallet.model.WalletTransaction
import com.example.foodieheal.wallet.model.WalletTransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object ReceiptShareUtil {
    private const val TAG = "ReceiptShareUtil"

    fun shareReceiptImage(context: Context, transaction: WalletTransaction) {
        try {
            val bitmap = generateReceiptBitmap(context, transaction)
            val cachePath = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(cachePath, "receipt_${transaction.id.take(8)}_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FoodieHeal Transaction Receipt")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "FoodieHeal Official Receipt\n" +
                            "Amount: RM ${String.format(Locale.US, "%.2f", transaction.safeAmount)}\n" +
                            "Ref ID: ${transaction.id}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt via"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing receipt", e)
            Toast.makeText(context, "Failed to generate receipt image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateReceiptBitmap(context: Context, txn: WalletTransaction): Bitmap {
        val width = 1080
        val height = 1750
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Soft background
        val bgPaint = Paint().apply { color = Color.parseColor("#F4F5F7") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Receipt Card
        val cardMarginX = 60f
        val cardMarginY = 70f
        val cardWidth = width - (cardMarginX * 2)
        val cardHeight = height - (cardMarginY * 2)
        val cardRect = RectF(cardMarginX, cardMarginY, cardMarginX + cardWidth, cardMarginY + cardHeight)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(25f, 0f, 10f, Color.parseColor("#20000000"))
        }
        canvas.drawRoundRect(cardRect, 36f, 36f, cardPaint)

        // Top Banner Header
        val headerHeight = 220f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardMarginX, cardMarginY, cardMarginX + cardWidth, cardMarginY + headerHeight,
                Color.parseColor("#EC5E3A"),
                Color.parseColor("#E64A19"),
                Shader.TileMode.CLAMP
            )
        }
        val headerPath = Path().apply {
            addRoundRect(
                cardMarginX, cardMarginY, cardMarginX + cardWidth, cardMarginY + headerHeight,
                floatArrayOf(36f, 36f, 36f, 36f, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(headerPath, headerPaint)

        // Header Text: FOODIEHEAL PAY
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FoodieHeal Pay", width / 2f, cardMarginY + 110f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0FFFFFF")
            textSize = 30f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Official E-Wallet Transaction Receipt", width / 2f, cardMarginY + 165f, subtitlePaint)

        // Status Badge Circle
        val isCredit = txn.isCredit
        val statusBgColor = if (isCredit) Color.parseColor("#E8F5E9") else Color.parseColor("#FFEBEE")
        val statusAccentColor = if (isCredit) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")

        val badgeCenterY = cardMarginY + headerHeight + 110f
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusBgColor }
        canvas.drawCircle(width / 2f, badgeCenterY, 60f, circlePaint)

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusAccentColor
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(if (isCredit) "↓" else "↑", width / 2f, badgeCenterY + 20f, checkPaint)

        // Status Label Pill
        val statusTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusAccentColor
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val statusLabel = when (txn.typeEnum) {
            WalletTransactionType.TOP_UP -> "Top-Up Successful"
            WalletTransactionType.APPOINTMENT_PAYMENT -> "Payment Successful"
            WalletTransactionType.REFUND -> "Refund Credited"
            WalletTransactionType.RESCHEDULE_ADJUSTMENT -> "Adjustment Applied"
        }
        canvas.drawText(statusLabel, width / 2f, badgeCenterY + 115f, statusTextPaint)

        // Amount
        val sign = if (isCredit) "+" else "-"
        val amountStr = "$sign RM ${String.format(Locale.US, "%.2f", txn.safeAmount)}"
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusAccentColor
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(amountStr, width / 2f, badgeCenterY + 210f, amountPaint)

        // Tng Style Dashed Divider with semicircles heihei
        val dividerY = badgeCenterY + 280f
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D5D7DB")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            pathEffect = DashPathEffect(floatArrayOf(16f, 16f), 0f)
        }
        canvas.drawLine(cardMarginX + 50f, dividerY, cardMarginX + cardWidth - 50f, dividerY, dashPaint)

        val cutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#F4F5F7") }
        canvas.drawCircle(cardMarginX, dividerY, 28f, cutoutPaint)
        canvas.drawCircle(cardMarginX + cardWidth, dividerY, 28f, cutoutPaint)

        // Detail Key-Value Rows
        var currentY = dividerY + 70f
        val rowHeight = 76f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757575")
            textSize = 34f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val leftX = cardMarginX + 60f
        val rightX = cardMarginX + cardWidth - 60f

        fun drawRow(label: String, value: String, isValueBold: Boolean = true, overrideValColor: Int? = null) {
            valPaint.color = overrideValColor ?: Color.parseColor("#212121")
            valPaint.typeface = if (isValueBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            canvas.drawText(label, leftX, currentY, labelPaint)
            val shortenedVal = if (value.length > 28) value.take(25) + "..." else value
            canvas.drawText(shortenedVal, rightX, currentY, valPaint)
            currentY += rowHeight
        }

        val formattedDate = formatReceiptDate(txn.createdAt)
        drawRow("Date & Time", formattedDate, isValueBold = false)
        drawRow("Transaction ID", txn.id.take(18) + "...", isValueBold = true)

        val paymentMethodDisplay = if (txn.typeEnum == WalletTransactionType.TOP_UP) {
            if (txn.paymentMethod != null) txn.paymentMethod.displayTitle else "Instant Top-Up Gateway"
        } else if (txn.typeEnum == WalletTransactionType.REFUND) {
            "In-App Wallet (Refund)"
        } else {
            "FoodieHeal In-App Wallet"
        }
        drawRow("Payment Method", paymentMethodDisplay)

        if (!txn.description.isNullOrBlank()) {
            drawRow("Description", txn.description)
        }

        drawRow("Balance Before", "RM ${String.format(Locale.US, "%.2f", txn.safeBalanceBefore)}", isValueBold = false)
        drawRow("Balance After", "RM ${String.format(Locale.US, "%.2f", txn.safeBalanceAfter)}", isValueBold = true, overrideValColor = android.graphics.Color.parseColor("#EC5E3A"))

        // Footer barcode & generated message
        val footerDividerY = currentY + 30f
        val lightDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EEEEEE")
            strokeWidth = 3f
        }
        canvas.drawLine(cardMarginX + 40f, footerDividerY, cardMarginX + cardWidth - 40f, footerDividerY, lightDividerPaint)

        // Draw decorative barcode lines
        val barcodeY = footerDividerY + 40f
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#333333") }
        val barPattern = intArrayOf(4, 2, 6, 2, 3, 5, 2, 6, 3, 2, 5, 3, 2, 6, 4, 2, 5, 3, 2, 4, 3, 6, 2, 4, 2, 5, 3, 6, 2, 4)
        var barStartX = (width / 2f) - 200f
        for (w in barPattern) {
            canvas.drawRect(barStartX, barcodeY, barStartX + w * 2.2f, barcodeY + 50f, barPaint)
            barStartX += (w * 2.2f) + 6f
        }

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9E9E")
            textSize = 26f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for choosing FoodieHeal", width / 2f, barcodeY + 95f, footerTextPaint)
        canvas.drawText("Computer-generated receipt • No signature required", width / 2f, barcodeY + 135f, footerTextPaint)

        return bitmap
    }

    private fun formatReceiptDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Recent"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString.substringBefore("."))
            if (date != null) {
                val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                formatter.format(date)
            } else {
                isoString
            }
        } catch (_: Exception) {
            isoString ?: "Recent"
        }
    }
}

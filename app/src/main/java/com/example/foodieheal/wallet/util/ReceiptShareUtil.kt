package com.example.foodieheal.wallet.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
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


    // PDF Export
    fun sharePdfReceipt(context: Context, transaction: WalletTransaction) {
        try {
            // A4 (standard PDF page size)
            val pageWidth  = 595
            val pageHeight = 842

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawReceiptOnCanvas(
                canvas    = canvas,
                txn       = transaction,
                width     = pageWidth,
                height    = pageHeight,
                scalePx   = 1f
            )

            document.finishPage(page)

            // Write to cache
            val cacheDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val pdfFile  = File(cacheDir, "receipt_${transaction.id.take(8)}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { document.writeTo(it) }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FoodieHeal Transaction Receipt")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "FoodieHeal Official Receipt (PDF)\n" +
                    "Amount: RM ${String.format(Locale.US, "%.2f", transaction.safeAmount)}\n" +
                    "Ref ID: ${transaction.id}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export PDF via"))
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF receipt", e)
            Toast.makeText(context, "Failed to generate PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawReceiptOnCanvas(
        canvas: Canvas,
        txn: WalletTransaction,
        width: Int,
        height: Int,
        scalePx: Float
    ) {
        val s = scalePx
        val w = width.toFloat()
        val h = height.toFloat()

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#F4F5F7") }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Card
        val mx = 30f * s; val my = 24f * s
        val cw = w - mx * 2; val ch = h - my * 2
        val cardRect = RectF(mx, my, mx + cw, my + ch)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(12f * s, 0f, 4f * s, Color.parseColor("#20000000"))
        }
        canvas.drawRoundRect(cardRect, 18f * s, 18f * s, cardPaint)

        // Header gradient
        val hh = 110f * s
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                mx, my, mx + cw, my + hh,
                Color.parseColor("#EC5E3A"), Color.parseColor("#E64A19"),
                Shader.TileMode.CLAMP
            )
        }
        val headerPath = Path().apply {
            addRoundRect(
                mx, my, mx + cw, my + hh,
                floatArrayOf(18f*s,18f*s,18f*s,18f*s,0f,0f,0f,0f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(headerPath, headerPaint)

        // Header text
        val cx = w / 2f
        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 26f*s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FoodieHeal Pay", cx, my + 52f*s, titleP)
        val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0FFFFFF"); textSize = 14f*s
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Official E-Wallet Transaction Receipt", cx, my + 82f*s, subP)

        // Status badge
        val isCredit = txn.isCredit
        val statusBg  = if (isCredit) Color.parseColor("#E8F5E9") else Color.parseColor("#FFEBEE")
        val statusFg  = if (isCredit) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        val badgeCY   = my + hh + 55f*s

        canvas.drawCircle(cx, badgeCY, 30f*s, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusBg })
        val iconP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusFg; textSize = 28f*s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(if (isCredit) "↓" else "↑", cx, badgeCY + 10f*s, iconP)

        // Status label
        val statusLabel = when (txn.typeEnum) {
            WalletTransactionType.TOP_UP              -> "Top-Up Successful"
            WalletTransactionType.APPOINTMENT_PAYMENT -> "Payment Successful"
            WalletTransactionType.REFUND              -> "Refund Credited"
            WalletTransactionType.RESCHEDULE_ADJUSTMENT -> "Adjustment Applied"
        }
        val statusP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusFg; textSize = 17f*s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(statusLabel, cx, badgeCY + 57f*s, statusP)

        // Amount
        val sign = if (isCredit) "+" else "-"
        val amtStr = "$sign RM ${String.format(Locale.US, "%.2f", txn.safeAmount)}"
        val amtP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusFg; textSize = 40f*s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawAmount(amtStr, cx, badgeCY + 105f*s, amtP)

        // Dashed divider with cutout circles
        val divY = badgeCY + 140f*s
        val dashP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D5D7DB"); style = Paint.Style.STROKE
            strokeWidth = 2f*s
            pathEffect = DashPathEffect(floatArrayOf(8f*s, 8f*s), 0f)
        }
        canvas.drawLine(mx + 25f*s, divY, mx + cw - 25f*s, divY, dashP)
        val cutP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F4F5F7") }
        canvas.drawCircle(mx, divY, 14f*s, cutP)
        canvas.drawCircle(mx + cw, divY, 14f*s, cutP)

        // Detail rows
        var rowY = divY + 35f*s
        val rowH = 38f*s
        val lx = mx + 30f*s; val rx = mx + cw - 30f*s
        val lblP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757575"); textSize = 16f*s
            textAlign = Paint.Align.LEFT
        }
        val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121"); textSize = 16f*s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        fun drawDetailRow(label: String, value: String, valColor: Int = Color.parseColor("#212121")) {
            valP.color = valColor
            canvas.drawText(label, lx, rowY, lblP)
            val v = if (value.length > 26) value.take(23) + "..." else value
            canvas.drawText(v, rx, rowY, valP)
            rowY += rowH
        }

        drawDetailRow("Date & Time", formatReceiptDate(txn.createdAt))
        drawDetailRow("Transaction ID", txn.id.take(14) + "...")
        val pmDisplay = when {
            txn.typeEnum == WalletTransactionType.TOP_UP && txn.paymentMethod != null -> txn.paymentMethod.displayTitle
            txn.typeEnum == WalletTransactionType.TOP_UP   -> "Instant Top-Up Gateway"
            txn.typeEnum == WalletTransactionType.REFUND   -> "In-App Wallet (Refund)"
            else -> "FoodieHeal In-App Wallet"
        }
        drawDetailRow("Payment Method", pmDisplay)
        if (!txn.description.isNullOrBlank()) drawDetailRow("Description", txn.description)
        drawDetailRow("Balance Before", "RM ${String.format(Locale.US, "%.2f", txn.safeBalanceBefore)}")
        drawDetailRow("Balance After",  "RM ${String.format(Locale.US, "%.2f", txn.safeBalanceAfter)}",
            Color.parseColor("#EC5E3A"))

        // Footer divider
        val fdY = rowY + 14f*s
        val fdP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EEEEEE"); strokeWidth = 1.5f*s }
        canvas.drawLine(mx + 20f*s, fdY, mx + cw - 20f*s, fdY, fdP)

        // Barcode
        val bcY = fdY + 20f*s
        val barP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#333333") }
        val pattern = intArrayOf(4,2,6,2,3,5,2,6,3,2,5,3,2,6,4,2,5,3,2,4,3,6,2,4,2,5,3,6,2,4)
        var bx = cx - 100f*s
        for (bw in pattern) {
            canvas.drawRect(bx, bcY, bx + bw * s, bcY + 24f*s, barP)
            bx += (bw * s) + 3f*s
        }

        // Footer text
        val ftP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9E9E"); textSize = 12f*s; textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for choosing FoodieHeal", cx, bcY + 40f*s, ftP)
        canvas.drawText("Computer-generated receipt • No signature required", cx, bcY + 60f*s, ftP)
    }

    private fun Canvas.drawAmount(text: String, x: Float, y: Float, paint: Paint) =
        this.drawText(text, x, y, paint)


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

package com.example.foodieheal.wallet.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.wallet.model.WalletTransaction
import com.example.foodieheal.wallet.model.WalletTransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun TransactionReceiptCard(
    transaction: WalletTransaction,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isCredit = transaction.isCredit
    val sign = if (isCredit) "+" else "-"
    val statusColor = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusBg = if (isCredit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val transactionIdLabel = stringResource(R.string.receipt_label_transaction_id)

    val formattedDate = formatReceiptDate(transaction.createdAt)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFEC5E3A),
                                Color(0xFFE64A19)
                            )
                        )
                    )
                    .padding(vertical = 18.dp, horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dollar_symbol),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.receipt_header_title),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = stringResource(R.string.receipt_header_subtitle),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (onShareClick != null) {
                        IconButton(
                            onClick = onShareClick,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = stringResource(R.string.share),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Status and amount section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Icon Circle
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(color = statusBg, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isCredit) R.drawable.ic_arrow_downward else R.drawable.ic_arrow_upward
                        ),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Status pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg,
                    contentColor = statusColor
                ) {
                    val statusLabel = when (transaction.typeEnum) {
                        WalletTransactionType.TOP_UP -> stringResource(R.string.receipt_status_top_up)
                        WalletTransactionType.APPOINTMENT_PAYMENT -> stringResource(R.string.receipt_status_payment)
                        WalletTransactionType.REFUND -> stringResource(R.string.receipt_status_refund)
                        WalletTransactionType.RESCHEDULE_ADJUSTMENT -> stringResource(R.string.receipt_status_adjustment)
                    }
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Amount
                Text(
                    text = "$sign RM ${String.format(Locale.US, "%.2f", transaction.safeAmount)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Cutout Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    val y = size.height / 2

                    // Dashed line across
                    drawLine(
                        color = Color(0xFFD5D7DB),
                        start = Offset(24.dp.toPx(), y),
                        end = Offset(size.width - 24.dp.toPx(), y),
                        strokeWidth = strokeWidth,
                        pathEffect = pathEffect
                    )

                    // Left & Right Semicircular Cutouts
                    drawCircle(
                        color = Color(0xFFF2F2F2),
                        radius = 12.dp.toPx(),
                        center = Offset(0f, y)
                    )
                    drawCircle(
                        color = Color(0xFFF2F2F2),
                        radius = 12.dp.toPx(),
                        center = Offset(size.width, y)
                    )
                }
            }

            // Receipt Details Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReceiptRow(
                    label = stringResource(R.string.receipt_label_transaction_type),
                    value = when (transaction.typeEnum) {
                        WalletTransactionType.TOP_UP -> stringResource(R.string.receipt_type_top_up)
                        WalletTransactionType.APPOINTMENT_PAYMENT -> stringResource(R.string.receipt_type_appointment)
                        WalletTransactionType.REFUND -> stringResource(R.string.receipt_type_refund)
                        WalletTransactionType.RESCHEDULE_ADJUSTMENT -> stringResource(R.string.receipt_type_adjustment)
                    }
                )

                // Transaction ID with Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transactionIdLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = transaction.id.take(14) + "...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(transactionIdLabel, transaction.id))
                                Toast.makeText(context, R.string.receipt_toast_copied, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy),
                                contentDescription = stringResource(R.string.receipt_cd_copy),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Payment Method
                val paymentMethodDisplay = if (transaction.typeEnum == WalletTransactionType.TOP_UP) {
                    if (transaction.paymentMethod != null) transaction.paymentMethod.displayTitle
                    else stringResource(R.string.receipt_method_gateway)
                } else if (transaction.typeEnum == WalletTransactionType.REFUND) {
                    stringResource(R.string.receipt_method_wallet_refund)
                } else {
                    stringResource(R.string.receipt_method_foodieheal_wallet)
                }
                ReceiptRow(label = stringResource(R.string.receipt_label_payment_source), value = paymentMethodDisplay)

                if (!transaction.description.isNullOrBlank()) {
                    ReceiptRow(label = stringResource(R.string.label_description), value = transaction.description)
                }

                if (!transaction.paymentId.isNullOrBlank()) {
                    ReceiptRow(label = stringResource(R.string.receipt_label_reference_id), value = transaction.paymentId)
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Balance summary
                ReceiptRow(
                    label = stringResource(R.string.receipt_label_balance_before),
                    value = "RM ${String.format(Locale.US, "%.2f", transaction.safeBalanceBefore)}"
                )
                ReceiptRow(
                    label = stringResource(R.string.receipt_label_balance_after),
                    value = "RM ${String.format(Locale.US, "%.2f", transaction.safeBalanceAfter)}",
                    valueColor = MaterialTheme.colorScheme.primary,
                    isBold = true
                )
            }

            // Decorative Barcode & Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Decorative Barcode lines
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(24.dp)
                ) {
                    val barWidths = listOf(3, 1, 4, 2, 1, 5, 2, 4, 1, 3, 2, 4, 1, 5, 3, 2, 4, 1, 3, 2)
                    barWidths.forEach { w ->
                        Box(
                            modifier = Modifier
                                .width(w.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.receipt_ref_format, transaction.id.take(20).uppercase()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.receipt_computer_generated_notice),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.4f)
        )
    }
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

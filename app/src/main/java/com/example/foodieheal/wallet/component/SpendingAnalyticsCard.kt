package com.example.foodieheal.wallet.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.wallet.model.WalletTransaction
import com.example.foodieheal.wallet.model.WalletTransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ColorPayment = Color(0xFFFF5722) // Payment Spend
private val ColorTopUp   = Color(0xFF2E7D32) //Top-Up Inflow
private val ColorRefund  = Color(0xFF1976D2) // Refund

data class MonthAnalyticsData(
    val monthKey: String,
    val monthLabel: String,
    val fullLabel: String,
    val payments: Double,
    val topUps: Double,
    val refunds: Double
) {
    val totalVolume: Double get() = payments + topUps + refunds
    val maxSingleCategory: Double get() = maxOf(payments, topUps, refunds)
    val netFlow: Double get() = (topUps + refunds) - payments
}

enum class AnalyticsTimeRange(val label: String, val monthCount: Int) {
    LAST_3_MONTHS("3M", 3),
    LAST_6_MONTHS("6M", 6),
    THIS_YEAR("12M", 12)
}

fun aggregateMonthlyAnalytics(
    transactions: List<WalletTransaction>,
    timeRange: AnalyticsTimeRange
): List<MonthAnalyticsData> {
    val calendar = Calendar.getInstance()
    val monthList = mutableListOf<Pair<String, Pair<String, String>>>() // key, (shortLabel, fullLabel)

    val keyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    val shortFormat = SimpleDateFormat("MMM", Locale.US)
    val fullFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    // Generate recent consecutive months in chronological order
    for (i in (timeRange.monthCount - 1) downTo 0) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -i)
        }
        val key = keyFormat.format(cal.time)
        val shortName = shortFormat.format(cal.time)
        val fullName = fullFormat.format(cal.time)
        monthList.add(key to (shortName to fullName))
    }

    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Group transactions by "yyyy-MM"
    val grouped = transactions.groupBy { txn ->
        try {
            val dateStr = txn.createdAt?.substringBefore(".") ?: ""
            val parsedDate = parser.parse(dateStr)
            if (parsedDate != null) keyFormat.format(parsedDate) else ""
        } catch (_: Exception) {
            ""
        }
    }

    return monthList.map { (key, labels) ->
        val txnsForMonth = grouped[key] ?: emptyList()
        var payments = 0.0
        var topUps = 0.0
        var refunds = 0.0

        for (txn in txnsForMonth) {
            val amt = txn.safeAmount
            when (txn.typeEnum) {
                WalletTransactionType.APPOINTMENT_PAYMENT -> payments += amt
                WalletTransactionType.TOP_UP -> topUps += amt
                WalletTransactionType.REFUND,
                WalletTransactionType.RESCHEDULE_ADJUSTMENT -> refunds += amt
            }
        }

        MonthAnalyticsData(
            monthKey = key,
            monthLabel = labels.first,
            fullLabel = labels.second,
            payments = payments,
            topUps = topUps,
            refunds = refunds
        )
    }
}


@Composable
fun SpendingAnalyticsCard(
    transactions: List<WalletTransaction>,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(AnalyticsTimeRange.LAST_6_MONTHS) }
    val monthlyData = remember(transactions, selectedRange) {
        aggregateMonthlyAnalytics(transactions, selectedRange)
    }

    // Default to the latest month
    var selectedMonthIndex by remember(monthlyData) {
        mutableIntStateOf((monthlyData.size - 1).coerceAtLeast(0))
    }

    val selectedMonth = monthlyData.getOrNull(selectedMonthIndex) ?: monthlyData.lastOrNull()

    val totalSpent = remember(monthlyData) { monthlyData.sumOf { it.payments } }
    val totalTopUp = remember(monthlyData) { monthlyData.sumOf { it.topUps } }
    val totalRefund = remember(monthlyData) { monthlyData.sumOf { it.refunds } }

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(selectedRange, transactions) {
        animationTriggered = true
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Range Selector (Flexible and non-colliding)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dollar_symbol),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Spending Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Monthly Breakdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Range Selector (3M | 6M | 12M) - Fixed single-line layout
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalyticsTimeRange.entries.forEach { range ->
                        val isSelected = selectedRange == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedRange = range }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = range.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Metric Summary Chips (Total Spent, Total Top-Up, Total Refund)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Spent",
                    amount = totalSpent,
                    accentColor = ColorPayment
                )
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Top-Up",
                    amount = totalTopUp,
                    accentColor = ColorTopUp
                )
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Refunds",
                    amount = totalRefund,
                    accentColor = ColorRefund
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = ColorPayment, label = "Payments")
                ChartLegendItem(color = ColorTopUp, label = "Top-Ups")
                ChartLegendItem(color = ColorRefund, label = "Refunds")
            }

            // Interactive Bar Chart
            val globalMax = remember(monthlyData) {
                val maxVal = monthlyData.maxOfOrNull { it.maxSingleCategory } ?: 0.0
                if (maxVal > 0) maxVal else 100.0
            }

            val chartScrollState = rememberScrollState()
            val isScrollable = selectedRange == AnalyticsTimeRange.THIS_YEAR

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                // Background reference grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 1.dp
                        )
                    }
                }

                // Month Bar Columns (with optional scroll if 12 months)
                Row(
                    modifier = if (isScrollable) {
                        Modifier
                            .fillMaxSize()
                            .horizontalScroll(chartScrollState)
                    } else {
                        Modifier.fillMaxSize()
                    },
                    horizontalArrangement = if (isScrollable) Arrangement.spacedBy(10.dp) else Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyData.forEachIndexed { index, monthData ->
                        val isSelected = index == selectedMonthIndex
                        MonthBarColumn(
                            monthData = monthData,
                            maxVal = globalMax,
                            isSelected = isSelected,
                            animated = animationTriggered,
                            isCompact = isScrollable,
                            onClick = { selectedMonthIndex = index }
                        )
                    }
                }
            }

            // Selected Month Detail Inspection Card
            if (selectedMonth != null) {
                SelectedMonthDetailView(selectedMonth = selectedMonth)
            }
        }
    }
}

/**
 * Single month bar column containing three grouped bars (Payments, Top-Ups, Refunds).
 */
@Composable
private fun MonthBarColumn(
    monthData: MonthAnalyticsData,
    maxVal: Double,
    isSelected: Boolean,
    animated: Boolean,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    val paymentHeightRatio = (monthData.payments / maxVal).toFloat().coerceIn(0f, 1f)
    val topUpHeightRatio = (monthData.topUps / maxVal).toFloat().coerceIn(0f, 1f)
    val refundHeightRatio = (monthData.refunds / maxVal).toFloat().coerceIn(0f, 1f)

    val animatedPayment by animateFloatAsState(
        targetValue = if (animated) paymentHeightRatio else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "anim_pay"
    )
    val animatedTopUp by animateFloatAsState(
        targetValue = if (animated) topUpHeightRatio else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "anim_topup"
    )
    val animatedRefund by animateFloatAsState(
        targetValue = if (animated) refundHeightRatio else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "anim_refund"
    )

    val maxHeightDp = 115.dp
    val barWidth = if (isCompact) 6.dp else 7.5.dp

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isCompact) 3.dp else 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Triple Bars grouped side-by-side
        Row(
            modifier = Modifier.height(maxHeightDp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
        ) {
            // Payment Bar
            SingleBar(
                width = barWidth,
                heightRatio = animatedPayment,
                maxHeight = maxHeightDp,
                barColor = ColorPayment,
                hasValue = monthData.payments > 0
            )

            // Top-Up Bar
            SingleBar(
                width = barWidth,
                heightRatio = animatedTopUp,
                maxHeight = maxHeightDp,
                barColor = ColorTopUp,
                hasValue = monthData.topUps > 0
            )

            // Refund Bar
            SingleBar(
                width = barWidth,
                heightRatio = animatedRefund,
                maxHeight = maxHeightDp,
                barColor = ColorRefund,
                hasValue = monthData.refunds > 0
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Month Label
        Text(
            text = monthData.monthLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.5.sp,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
private fun SingleBar(
    width: androidx.compose.ui.unit.Dp,
    heightRatio: Float,
    maxHeight: androidx.compose.ui.unit.Dp,
    barColor: Color,
    hasValue: Boolean
) {
    val barHeight = (maxHeight * heightRatio).coerceAtLeast(if (hasValue) 4.dp else 2.dp)
    Box(
        modifier = Modifier
            .width(width)
            .height(barHeight)
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .background(if (hasValue) barColor else barColor.copy(alpha = 0.15f))
    )
}

/**
 * Metric summary card for top stats.
 */
@Composable
private fun MetricSummaryCard(
    title: String,
    amount: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, "RM %.2f", amount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Legend indicator.
 */
@Composable
private fun ChartLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            softWrap = false
        )
    }
}

/**
 * Detailed breakout view for the currently selected month.
 */
@Composable
private fun SelectedMonthDetailView(
    selectedMonth: MonthAnalyticsData
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedMonth.fullLabel} Breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val isPositiveFlow = selectedMonth.netFlow >= 0
                Text(
                    text = "Net: ${if (isPositiveFlow) "+" else ""}RM ${String.format(Locale.US, "%.2f", selectedMonth.netFlow)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositiveFlow) ColorTopUp else ColorPayment,
                    softWrap = false
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MonthDetailRow(
                    modifier = Modifier.weight(1f),
                    label = "Payments",
                    amount = selectedMonth.payments,
                    color = ColorPayment
                )
                MonthDetailRow(
                    modifier = Modifier.weight(1f),
                    label = "Top-Ups",
                    amount = selectedMonth.topUps,
                    color = ColorTopUp
                )
                MonthDetailRow(
                    modifier = Modifier.weight(1f),
                    label = "Refunds",
                    amount = selectedMonth.refunds,
                    color = ColorRefund
                )
            }
        }
    }
}

@Composable
private fun MonthDetailRow(
    label: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = String.format(Locale.US, "RM %.2f", amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

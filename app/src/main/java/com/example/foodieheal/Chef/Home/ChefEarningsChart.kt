package com.example.foodieheal.Chef.Home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.hiring.model.AppointmentPricingBreakdown
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class EarningsTimeRange(val label: String, val monthCount: Int) {
    LAST_3_MONTHS("3M", 3),
    LAST_6_MONTHS("6M", 6),
    THIS_YEAR("12M", 12)
}

data class MonthEarnings(
    val monthKey: String,
    val monthLabel: String,
    val fullLabel: String,
    val grossEarnings: Double,
    val platformFee: Double,
    val earnings: Double,
    val appointmentCount: Int
)

fun aggregateChefEarnings(
    appointments: List<Appointment>,
    monthCount: Int = 6
): List<MonthEarnings> {
    val keyFmt   = SimpleDateFormat("yyyy-MM",       Locale.US)
    val shortFmt = SimpleDateFormat("MMM",           Locale.US)
    val fullFmt  = SimpleDateFormat("MMMM yyyy",     Locale.US)
    val dateFmt  = SimpleDateFormat("yyyy-MM-dd",    Locale.US)

    // Build the ordered month buckets
    val monthKeys = (monthCount - 1 downTo 0).map { offset ->
        Calendar.getInstance().apply { add(Calendar.MONTH, -offset) }
    }.map { cal ->
        Triple(keyFmt.format(cal.time), shortFmt.format(cal.time), fullFmt.format(cal.time))
    }

    // Only count completed / confirmed appointments
    val relevant = appointments.filter {
        val s = it.Status.lowercase()
        s == "completed" || s == "confirmed"
    }

    val grouped = relevant.groupBy { appt ->
        try {
            val parsed = dateFmt.parse(appt.Date)
            if (parsed != null) keyFmt.format(parsed) else ""
        } catch (_: Exception) { "" }
    }

    val feeRate = AppointmentPricingBreakdown.PLATFORM_FEE_RATE

    return monthKeys.map { (key, short, full) ->
        val bucket = grouped[key] ?: emptyList()
        val grossTotal = bucket.sumOf { it.Total_Price }
        // Net payout after deducting 5% platform fee
        val netEarnings = if (grossTotal > 0.0) grossTotal / (1.0 + feeRate) else 0.0
        val totalFee = grossTotal - netEarnings

        MonthEarnings(
            monthKey         = key,
            monthLabel       = short,
            fullLabel        = full,
            grossEarnings    = grossTotal,
            platformFee      = totalFee,
            earnings         = netEarnings,
            appointmentCount = bucket.size
        )
    }
}

@Composable
fun ChefEarningsChart(
    appointments: List<Appointment>,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(EarningsTimeRange.LAST_6_MONTHS) }

    val monthlyData = remember(appointments, selectedRange) {
        aggregateChefEarnings(appointments, monthCount = selectedRange.monthCount)
    }

    var selectedIndex by remember(monthlyData) {
        mutableIntStateOf((monthlyData.size - 1).coerceAtLeast(0))
    }

    val selectedMonth = monthlyData.getOrNull(selectedIndex) ?: monthlyData.lastOrNull()

    val totalGross = remember(monthlyData) { monthlyData.sumOf { it.grossEarnings } }
    val totalFee = remember(monthlyData) { monthlyData.sumOf { it.platformFee } }
    val totalEarnings = remember(monthlyData) { monthlyData.sumOf { it.earnings } }

    var animTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(selectedRange, appointments) { animTriggered = true }

    val globalMax = remember(monthlyData) {
        val m = monthlyData.maxOfOrNull { it.earnings } ?: 0.0
        if (m > 0.0) m else 100.0
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val chartScrollState = rememberScrollState()
    val isScrollable = selectedRange == EarningsTimeRange.THIS_YEAR

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
            // Header Row: Range Selector (3m / 6m / 12m)
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
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.wallet),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = stringResource(R.string.chef_earnings_overview),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.chef_earnings_last_months_format, selectedRange.monthCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Range Selector (3M | 6M | 12M)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EarningsTimeRange.entries.forEach { range ->
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

            // Summary Metric Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EarningsMetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.chef_earnings_total_gross),
                    amount = totalGross,
                    accentColor = MaterialTheme.colorScheme.onSurface
                )
                EarningsMetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.chef_earnings_platform_fee),
                    amount = totalFee,
                    accentColor = MaterialTheme.colorScheme.error
                )
                EarningsMetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.chef_earnings_net_payout),
                    amount = totalEarnings,
                    accentColor = accentColor
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bar Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
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

                // Month Bars (with horizontal scrolling when 12M is selected)
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
                        EarningsBarColumn(
                            monthData    = monthData,
                            maxVal       = globalMax,
                            isSelected   = index == selectedIndex,
                            animated     = animTriggered,
                            accentColor  = accentColor,
                            isCompact    = isScrollable,
                            onClick      = { selectedIndex = index }
                        )
                    }
                }
            }

            // Selected Month Detail Inspection Card
            if (selectedMonth != null) {
                EarningsDetailCard(
                    month       = selectedMonth,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun EarningsMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
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

@Composable
private fun EarningsBarColumn(
    monthData:   MonthEarnings,
    maxVal:      Double,
    isSelected:  Boolean,
    animated:    Boolean,
    accentColor: Color,
    isCompact:   Boolean = false,
    onClick:     () -> Unit
) {
    val ratio = (monthData.earnings / maxVal).toFloat().coerceIn(0f, 1f)
    val animatedHeight by animateFloatAsState(
        targetValue    = if (animated) ratio else 0f,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "earnings_bar"
    )
    val maxBarDp = 110.dp
    val barWidth = if (isCompact) 16.dp else 18.dp

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isCompact) 4.dp else 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Bar
        Box(
            modifier = Modifier
                .height(maxBarDp)
                .width(barWidth),
            contentAlignment = Alignment.BottomCenter
        ) {
            val barHeight = (maxBarDp * animatedHeight)
                .coerceAtLeast(if (monthData.earnings > 0) 4.dp else 2.dp)
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .background(
                        if (monthData.earnings > 0) {
                            if (isSelected) accentColor else accentColor.copy(alpha = 0.55f)
                        } else {
                            accentColor.copy(alpha = 0.15f)
                        }
                    )
            )
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text     = monthData.monthLabel,
            fontSize = if (isCompact) 10.sp else 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color    = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
private fun EarningsDetailCard(
    month: MonthEarnings,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = stringResource(R.string.chef_earnings_breakdown_format, month.fullLabel),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val apptCountStr = if (month.appointmentCount == 1) {
                    stringResource(R.string.chef_earnings_appointment_single)
                } else {
                    stringResource(R.string.chef_earnings_appointment_plural_format, month.appointmentCount)
                }
                val feeStr = if (month.grossEarnings > 0) {
                    stringResource(R.string.chef_earnings_fee_deduction_format, month.platformFee)
                } else ""
                Text(
                    text = apptCountStr + feeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.chef_earnings_net_payout),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.US, "RM %.2f", month.earnings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (month.earnings > 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

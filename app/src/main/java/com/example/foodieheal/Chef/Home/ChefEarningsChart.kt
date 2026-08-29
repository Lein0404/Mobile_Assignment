package com.example.foodieheal.Chef.Home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.hiring.model.Appointment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.example.foodieheal.hiring.model.AppointmentPricingBreakdown

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
    val monthlyData = remember(appointments) {
        aggregateChefEarnings(appointments, monthCount = 6)
    }

    var selectedIndex by remember(monthlyData) {
        mutableIntStateOf((monthlyData.size - 1).coerceAtLeast(0))
    }

    val selectedMonth = monthlyData.getOrNull(selectedIndex) ?: monthlyData.lastOrNull()

    val totalEarnings = remember(monthlyData) { monthlyData.sumOf { it.earnings } }

    var animTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(appointments) { animTriggered = true }

    val globalMax = remember(monthlyData) {
        val m = monthlyData.maxOfOrNull { it.earnings } ?: 0.0
        if (m > 0.0) m else 100.0
    }

    val accentColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Earnings Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Last 6 months • Net Payout (excl. 5% platform fee)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Total pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Net Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = String.format(Locale.US, "RM %.2f", totalEarnings),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            softWrap = false
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                // Grid lines
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

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyData.forEachIndexed { index, monthData ->
                        EarningsBarColumn(
                            monthData    = monthData,
                            maxVal       = globalMax,
                            isSelected   = index == selectedIndex,
                            animated     = animTriggered,
                            accentColor  = accentColor,
                            onClick      = { selectedIndex = index }
                        )
                    }
                }
            }

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
private fun EarningsBarColumn(
    monthData:   MonthEarnings,
    maxVal:      Double,
    isSelected:  Boolean,
    animated:    Boolean,
    accentColor: Color,
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

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Bar
        Box(
            modifier = Modifier
                .height(maxBarDp)
                .width(18.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val barHeight = (maxBarDp * animatedHeight)
                .coerceAtLeast(if (monthData.earnings > 0) 4.dp else 2.dp)
            Box(
                modifier = Modifier
                    .width(18.dp)
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
            fontSize = 10.5.sp,
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
                    text = month.fullLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${month.appointmentCount} appointment${if (month.appointmentCount != 1) "s" else ""}" +
                            if (month.grossEarnings > 0) " • Fee: -RM %.2f".format(Locale.US, month.platformFee) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Net Payout",
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

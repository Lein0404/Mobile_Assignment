package com.example.foodieheal.Payment.Screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.example.foodieheal.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.hiring.model.AppointmentPricingBreakdown
import java.text.SimpleDateFormat
import java.util.Locale

data class PriceBreakdown(
    val baseRate: Double,
    val recipesAddOn: Double,
    val platformCharge: Double
) {
    // Backward compatibility alias
    val servingPremium: Double get() = platformCharge

    val total: Double get() = baseRate + recipesAddOn + platformCharge

    val baseFraction: Float     get() = if (total <= 0) 0f else (baseRate / total).toFloat()
    val recipesFraction: Float  get() = if (total <= 0) 0f else (recipesAddOn / total).toFloat()
    val premiumFraction: Float  get() = if (total <= 0) 0f else (platformCharge / total).toFloat()
    val platformFraction: Float get() = premiumFraction
}

/**
 * - base rate: hourly rate amount (labor cost = hourly rate * hours)
 * - recipe add: recipe price + ingredient cost (+ travel surcharge if any)
 * - platform charge: platform fee (5% commission on subtotal)
 */
fun deriveBreakdown(
    totalPrice: Double,
    startTime: String? = null,
    endTime: String? = null,
    servingSize: Int = 1,
    hourlyRate: Double? = null,
    recipePrice: Double? = null,
    ingredientCost: Double? = null,
    pricingBreakdown: AppointmentPricingBreakdown? = null
): PriceBreakdown {
    if (pricingBreakdown != null && pricingBreakdown.finalTotalPrice > 0) {
        return PriceBreakdown(
            baseRate       = pricingBreakdown.laborCost,
            recipesAddOn   = pricingBreakdown.ingredientsCost + pricingBreakdown.travelSurcharge,
            platformCharge = pricingBreakdown.platformFee
        )
    }

    if (totalPrice <= 0.0) return PriceBreakdown(0.0, 0.0, 0.0)

    val hours = parseHours(startTime, endTime) ?: 1.0

    // Exact platform fee formula from AppointmentPricingBreakdown:
    // Total = Subtotal * (1 + PLATFORM_FEE_RATE)
    // Subtotal = Total / (1 + 0.05)
    // Platform Fee = Total - Subtotal
    val feeRate = AppointmentPricingBreakdown.PLATFORM_FEE_RATE
    val subtotal = (totalPrice / (1.0 + feeRate)).coerceAtLeast(0.0)
    val platformCharge = (totalPrice - subtotal).coerceAtLeast(0.0)

    // Base Rate (Labor Cost = hourlyRate * hours) and Recipe Add (Recipe + Ingredients)
    val base: Double
    val recipeAdd: Double

    if (hourlyRate != null && hourlyRate > 0.0) {
        val calculatedBase = hourlyRate * hours
        base = calculatedBase.coerceIn(0.0, subtotal)
        recipeAdd = (subtotal - base).coerceAtLeast(0.0)
    } else if (recipePrice != null || ingredientCost != null) {
        val recipeIngredientTotal = (recipePrice ?: 0.0) + (ingredientCost ?: 0.0)
        recipeAdd = recipeIngredientTotal.coerceIn(0.0, subtotal)
        base = (subtotal - recipeAdd).coerceAtLeast(0.0)
    } else {
        // When only total price and time are provided, allocate hourly rate amount proportional to duration
        val hoursWeight = (hours / (hours + 0.8)).coerceIn(0.40, 0.75)
        base = subtotal * hoursWeight
        recipeAdd = (subtotal - base).coerceAtLeast(0.0)
    }

    return PriceBreakdown(
        baseRate       = base,
        recipesAddOn   = recipeAdd,
        platformCharge = platformCharge
    )
}

private fun parseHours(startTime: String?, endTime: String?): Double? {
    if (startTime.isNullOrBlank() || endTime.isNullOrBlank()) return null
    return try {
        AppointmentPricingBreakdown.calculateHours(startTime, endTime)
    } catch (_: Exception) {
        val patterns = listOf("hh:mm a", "h:mm a", "HH:mm:ss", "HH:mm", "H:mm:ss", "H:mm")
        for (pattern in patterns) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US)
                val start = fmt.parse(startTime.trim()) ?: continue
                val end   = fmt.parse(endTime.trim())   ?: continue
                val diffMs = end.time - start.time
                if (diffMs > 0) return diffMs / 3_600_000.0
            } catch (_: Exception) {}
        }
        null
    }
}

private val ColorBase    = Color(0xFF5C6BC0) // Indigo - Base Rate (Hourly Rate Amount)
private val ColorRecipes = Color(0xFF2E7D32) // Forest Green - Recipe Add (Recipe Price + Ingredient)
private val ColorPremium = Color(0xFFF57C00) // Amber/Orange - Premium Rate (Platform Charge)

@Composable
fun AnimatedPriceBreakdownBar(
    totalPrice: Double,
    startTime: String?,
    endTime: String?,
    servingSize: Int = 1,
    hourlyRate: Double? = null,
    recipePrice: Double? = null,
    ingredientCost: Double? = null,
    pricingBreakdown: AppointmentPricingBreakdown? = null,
    modifier: Modifier = Modifier
) {
    val breakdown = remember(totalPrice, startTime, endTime, servingSize, hourlyRate, recipePrice, ingredientCost, pricingBreakdown) {
        deriveBreakdown(
            totalPrice       = totalPrice,
            startTime        = startTime,
            endTime          = endTime,
            servingSize      = servingSize,
            hourlyRate       = hourlyRate,
            recipePrice      = recipePrice,
            ingredientCost   = ingredientCost,
            pricingBreakdown = pricingBreakdown
        )
    }

    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }

    val animBase by animateFloatAsState(
        targetValue = if (triggered) breakdown.baseFraction else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "base_anim"
    )
    val animRecipes by animateFloatAsState(
        targetValue = if (triggered) breakdown.recipesFraction else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "recipes_anim"
    )
    val animPremium by animateFloatAsState(
        targetValue = if (triggered) breakdown.premiumFraction else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "premium_anim"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.breakdown_title_price_breakdown),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (animBase > 0f) {
                Box(
                    modifier = Modifier
                        .weight(animBase)
                        .height(14.dp)
                        .background(ColorBase)
                )
            }
            if (animRecipes > 0f) {
                Box(
                    modifier = Modifier
                        .weight(animRecipes)
                        .height(14.dp)
                        .background(ColorRecipes)
                )
            }
            if (animPremium > 0f) {
                Box(
                    modifier = Modifier
                        .weight(animPremium)
                        .height(14.dp)
                        .background(ColorPremium)
                )
            }

            val remainingWeight = (1f - animBase - animRecipes - animPremium).coerceAtLeast(0.0001f)
            if (remainingWeight > 0.001f) {
                Box(modifier = Modifier.weight(remainingWeight))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend rows
        // Base Rate (Hourly Rate Amount / Labor Cost)
        BreakdownLegendRow(
            color      = ColorBase,
            label      = stringResource(R.string.breakdown_base_rate),
            amount     = breakdown.baseRate,
            percentage = breakdown.baseFraction
        )

        // Recipe Add (Recipe Price + Ingredient Cost)
        if (breakdown.recipesAddOn > 0) {
            BreakdownLegendRow(
                color      = ColorRecipes,
                label      = stringResource(R.string.breakdown_recipes_add_on),
                amount     = breakdown.recipesAddOn,
                percentage = breakdown.recipesFraction
            )
        }

        // Premium Rate (Platform Charge)
        if (breakdown.platformCharge > 0) {
            BreakdownLegendRow(
                color      = ColorPremium,
                label      = stringResource(R.string.breakdown_platform_charge),
                amount     = breakdown.platformCharge,
                percentage = breakdown.premiumFraction
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.breakdown_total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.breakdown_currency_format, String.format(Locale.US, "%.2f", totalPrice)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BreakdownLegendRow(
    color: Color,
    label: String,
    amount: Double,
    percentage: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = stringResource(R.string.breakdown_currency_format, String.format(Locale.US, "%.2f", amount)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

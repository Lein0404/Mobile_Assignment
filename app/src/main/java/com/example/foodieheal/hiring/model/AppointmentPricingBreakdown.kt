package com.example.foodieheal.hiring.model

import com.example.foodieheal.Recipe.Model.Recipe
import java.text.SimpleDateFormat
import java.util.Locale

data class RecipeCostItem(
    val recipeId: String?,
    val recipeName: String,
    val baseEstimatedCost: Double,
    val portions: Int,
    val totalCost: Double
)


 // Formula = Total Price = (Labor Cost + Ingredients Cost + Travel Surcharge) * (1 + Platform Fee Rate)
data class AppointmentPricingBreakdown(
    val hourlyRate: Double = 0.0,
    val hours: Double = 1.0,
    val laborCost: Double = 0.0,
    val recipeCostItems: List<RecipeCostItem> = emptyList(),
    val ingredientsCost: Double = 0.0,
    val userState: String = "",
    val chefState: String = "",
    val isInterstate: Boolean = false,
    val travelSurcharge: Double = 0.0,
    val subtotal: Double = 0.0,
    val platformFeeRate: Double = PLATFORM_FEE_RATE,
    val platformFee: Double = 0.0,
    val finalTotalPrice: Double = 0.0
) {
    companion object {
        const val INTERSTATE_SURCHARGE_AMOUNT = 30.0
        const val PLATFORM_FEE_RATE = 0.05 // 5% platform commission

        fun calculate(
            chefHourlyRate: Double,
            appointmentTime: String,
            selectedRecipes: List<SelectedAppointmentRecipe>,
            userState: String,
            chefState: String
        ): AppointmentPricingBreakdown {
            // Labor Cost = chefHourlyRate * appointmentHours
            val hours = calculateHours(appointmentTime)
            val laborCost = chefHourlyRate * hours

            // Ingredients Cost (scaled per serving)
            val costItems = selectedRecipes.map { item ->
                val baseEstimated = parseEstimatedBudget(item.recipe.estimatedBudget)
                val portions = item.serviceCount.coerceAtLeast(1)
                val itemTotal = baseEstimated * portions
                RecipeCostItem(
                    recipeId = item.recipe.recipe_id,
                    recipeName = item.recipe.recipeName,
                    baseEstimatedCost = baseEstimated,
                    portions = portions,
                    totalCost = itemTotal
                )
            }
            val ingredientsCost = costItems.sumOf { it.totalCost }

            // Interstate Travel Surcharge
            val cleanUserState = userState.trim()
            val cleanChefState = chefState.trim()
            val isInterstate = cleanUserState.isNotBlank() && cleanChefState.isNotBlank() &&
                    !cleanUserState.equals(cleanChefState, ignoreCase = true)
            val travelSurcharge = if (isInterstate) INTERSTATE_SURCHARGE_AMOUNT else 0.0

            // Platform / Service Fee
            val subtotal = laborCost + ingredientsCost + travelSurcharge
            val platformFee = subtotal * PLATFORM_FEE_RATE
            val finalTotalPrice = subtotal + platformFee

            return AppointmentPricingBreakdown(
                hourlyRate = chefHourlyRate,
                hours = hours,
                laborCost = laborCost,
                recipeCostItems = costItems,
                ingredientsCost = ingredientsCost,
                userState = cleanUserState,
                chefState = cleanChefState,
                isInterstate = isInterstate,
                travelSurcharge = travelSurcharge,
                subtotal = subtotal,
                platformFeeRate = PLATFORM_FEE_RATE,
                platformFee = platformFee,
                finalTotalPrice = finalTotalPrice
            )
        }

        fun parseEstimatedBudget(budgetStr: String?): Double {
            if (budgetStr.isNullOrBlank()) return 0.0
            val clean = budgetStr
                .replace("RM", "", ignoreCase = true)
                .replace("$", "")
                .replace(",", "")
                .trim()
            return clean.toDoubleOrNull() ?: 0.0
        }

        fun calculateHours(appointmentTime: String): Double {
            if (!appointmentTime.contains("-")) return 1.0
            val parts = appointmentTime.split("-").map { it.trim() }
            if (parts.size != 2) return 1.0

            val patterns = listOf("hh:mm a", "h:mm a", "HH:mm:ss", "HH:mm", "H:mm:ss", "H:mm")
            for (p in patterns) {
                try {
                    val sdf = SimpleDateFormat(p, Locale.US)
                    val start = sdf.parse(parts[0])
                    val end = sdf.parse(parts[1])
                    if (start != null && end != null) {
                        val diffMillis = end.time - start.time
                        val diffHours = diffMillis.toDouble() / (1000 * 60 * 60)
                        return if (diffHours > 0) diffHours else 1.0
                    }
                } catch (_: Exception) {}
            }
            return 1.0
        }
    }
}

package com.example.foodieheal.hiring.model

import com.example.foodieheal.Recipe.Model.Recipe
import java.text.SimpleDateFormat
import java.util.Locale

data class RecipeCostItem(
    val recipeId: String?,
    val recipeName: String,
    val baseEstimatedCost: Double,
    val portions: Int,
    val totalCost: Double,
    val chefProvidesIngredients: Boolean = true
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
            // Labor cost = chefHourlyRate * appointmentHours
            val hours = calculateHours(appointmentTime)
            val laborCost = chefHourlyRate * hours

            // Ingredients cost (scaled per serving only if chef provides ingredients)
            val costItems = selectedRecipes.map { item ->
                val baseEstimated = parseEstimatedBudget(item.recipe.estimatedBudget)
                val portions = item.serviceCount.coerceAtLeast(1)
                val itemTotal = if (item.chefProvidesIngredients) baseEstimated * portions else 0.0
                RecipeCostItem(
                    recipeId = item.recipe.recipe_id,
                    recipeName = item.recipe.recipeName,
                    baseEstimatedCost = baseEstimated,
                    portions = portions,
                    totalCost = itemTotal,
                    chefProvidesIngredients = item.chefProvidesIngredients
                )
            }
            val ingredientsCost = costItems.sumOf { it.totalCost }

            // Interstate Travel Surcharge
            val cleanUserState = userState.trim()
            val cleanChefState = chefState.trim()
            val isInterstate = cleanUserState.isNotBlank() && cleanChefState.isNotBlank() &&
                    !cleanUserState.equals(cleanChefState, ignoreCase = true)
            val travelSurcharge = if (isInterstate) INTERSTATE_SURCHARGE_AMOUNT else 0.0

            // Service Fee
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
            val clean = budgetStr.trim()

            // Find all numbers (integers or decimals) in the string
            val numberRegex = Regex("""\d+(?:\.\d+)?""")
            val matches = numberRegex.findAll(clean).mapNotNull { it.value.toDoubleOrNull() }.toList()

            return when {
                matches.isEmpty() -> 0.0
                matches.size >= 2 -> {
                    // Range format 20 - 40 -> average (20 + 40) / 2 = 30
                    (matches[0] + matches[1]) / 2.0
                }
                else -> matches[0]
            }
        }

        fun parseTimeToMinutes(timeStr: String?): Int? {
            if (timeStr.isNullOrBlank()) return null
            val clean = timeStr.trim().uppercase(Locale.US)

            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")

            // Extract digits and colon only
            val digitsOnly = clean.replace(Regex("""[^0-9:]"""), "").trim()
            val parts = digitsOnly.split(":")
            if (parts.isEmpty()) return null

            val rawHour = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            var hour24 = rawHour
            if (isPm || isAm) {
                if (isPm && rawHour < 12) {
                    hour24 = rawHour + 12
                } else if (isAm && rawHour == 12) {
                    hour24 = 0
                }
            }

            return (hour24 * 60 + minute).coerceIn(0, 1439)
        }

        fun calculateHours(startStr: String?, endStr: String?): Double {
            val startMin = parseTimeToMinutes(startStr) ?: return 1.0
            val endMin = parseTimeToMinutes(endStr) ?: return 1.0

            val diffMinutes = if (endMin >= startMin) {
                endMin - startMin
            } else {
                (endMin + 24 * 60) - startMin // Handles crossing midnight
            }
            val hours = diffMinutes / 60.0
            return if (hours > 0.0) hours else 1.0
        }

        fun parseTimeToCalendar(timeStr: String?): java.util.Calendar? {
            val totalMinutes = parseTimeToMinutes(timeStr) ?: return null
            val hour = totalMinutes / 60
            val minute = totalMinutes % 60
            return java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
        }

        fun parseTimeSlotToCalendars(startTimeStr: String?, endTimeStr: String?): Pair<java.util.Calendar, java.util.Calendar>? {
            val startCal = parseTimeToCalendar(startTimeStr) ?: return null
            val endCal = parseTimeToCalendar(endTimeStr) ?: return null
            return Pair(startCal, endCal)
        }

        fun calculateHours(appointmentTime: String): Double {
            if (!appointmentTime.contains("-")) return 1.0
            val parts = appointmentTime.split("-").map { it.trim() }
            if (parts.size != 2) return 1.0
            return calculateHours(parts[0], parts[1])
        }
    }
}

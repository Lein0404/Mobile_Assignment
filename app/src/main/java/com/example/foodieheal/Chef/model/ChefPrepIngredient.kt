package com.example.foodieheal.Chef.model

import com.example.foodieheal.Recipe.Model.IngredientItem
import com.example.foodieheal.hiring.model.AppointmentRecipeWithDetails
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

@Serializable
data class ChefPrepIngredient(
    val id: String,
    val name: String,
    val totalQuantity: Double,
    val unit: String,
    val displayQuantity: String,
    val recipeNames: List<String>,
    val isChefProvided: Boolean,
    val isChecked: Boolean = false
)

object ChefPrepAggregator {

    fun aggregate(
        attachedRecipes: List<AppointmentRecipeWithDetails>,
        checkedIds: Set<String> = emptySet()
    ): List<ChefPrepIngredient> {
        if (attachedRecipes.isEmpty()) return emptyList()

        // Normalized (Name, Unit, isChefProvided)
        val groupedMap = mutableMapOf<String, AggregatedAccumulator>()

        for (item in attachedRecipes) {
            val recipe = item.recipe ?: continue
            val dishName = recipe.recipeName.ifBlank { "Attached Dish" }
            val portion = if (item.service_count > 0) item.service_count else 1.0
            val isChefProvided = item.chef_provide_ingredient

            for (ingredient in recipe.ingredients) {
                val rawName = ingredient.name.trim()
                if (rawName.isBlank()) continue

                val normalizedName = rawName.lowercase(Locale.ROOT)
                val rawUnit = ingredient.unit.trim()
                val normalizedUnit = rawUnit.lowercase(Locale.ROOT)
                val isChefProvidedKey = isChefProvided

                val key = "${normalizedName}_${normalizedUnit}_$isChefProvidedKey"
                val parsedQty = parseQuantity(ingredient) * portion

                val dishLabel = if (portion != 1.0) {
                    val portionStr = if (portion % 1.0 == 0.0) portion.toInt().toString() else String.format(Locale.US, "%.1f", portion)
                    "$dishName (x$portionStr)"
                } else {
                    dishName
                }

                val existing = groupedMap[key]
                if (existing == null) {
                    groupedMap[key] = AggregatedAccumulator(
                        key = key,
                        displayName = capitalizeWords(rawName),
                        totalQty = parsedQty,
                        unit = rawUnit,
                        dishes = mutableListOf(dishLabel),
                        isChefProvided = isChefProvided
                    )
                } else {
                    existing.totalQty += parsedQty
                    if (!existing.dishes.contains(dishLabel)) {
                        existing.dishes.add(dishLabel)
                    }
                }
            }
        }

        return groupedMap.values.map { acc ->
            val formattedQty = formatQuantity(acc.totalQty)
            val displayQty = if (acc.unit.isNotBlank()) {
                "$formattedQty ${acc.unit}".trim()
            } else {
                formattedQty
            }

            ChefPrepIngredient(
                id = acc.key,
                name = acc.displayName,
                totalQuantity = acc.totalQty,
                unit = acc.unit,
                displayQuantity = displayQty,
                recipeNames = acc.dishes.toList(),
                isChefProvided = acc.isChefProvided,
                isChecked = acc.isChefProvided && checkedIds.contains(acc.key)
            )
        }.sortedWith(
            compareByDescending<ChefPrepIngredient> { it.isChefProvided } // Chef provided items first
                .thenBy { it.isChecked }                                 // Unchecked first among chef items
                .thenBy { it.name }
        )
    }

    private fun parseQuantity(ingredient: IngredientItem): Double {
        return try {
            val content = when (val q = ingredient.quantity) {
                is JsonPrimitive -> q.content.trim()
                else -> q.toString().trim().replace("\"", "")
            }

            if (content.isBlank()) return 1.0

            // Fraction handling: e.g. "1/2", "3/4"
            if (content.contains("/")) {
                val parts = content.split("/")
                if (parts.size == 2) {
                    val num = parts[0].trim().toDoubleOrNull()
                    val den = parts[1].trim().toDoubleOrNull()
                    if (num != null && den != null && den != 0.0) {
                        return num / den
                    }
                }
            }

            // Numeric parsing
            content.toDoubleOrNull() ?: 1.0
        } catch (_: Exception) {
            1.0
        }
    }

    private fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", qty)
        }
    }

    private fun capitalizeWords(str: String): String {
        return str.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
    }

    private data class AggregatedAccumulator(
        val key: String,
        val displayName: String,
        var totalQty: Double,
        val unit: String,
        val dishes: MutableList<String>,
        val isChefProvided: Boolean
    )
}

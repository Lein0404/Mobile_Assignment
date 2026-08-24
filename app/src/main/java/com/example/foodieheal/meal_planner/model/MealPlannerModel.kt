package com.example.foodieheal.meal_planner.model

import com.example.foodieheal.model.Recipe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

// --- DB Data Transfer Objects ---
@Serializable
data class DailyPlanDTO(
    @SerialName("user_id") val userId: String,
    val date: String,
    val meals: List<MealSlotDTO>? = emptyList()
)

@Serializable
data class MealSlotDTO(
    @SerialName("mealType") val mealType: MealType,
    val recipes: List<RecipeReference> = emptyList()
)

@Serializable
data class RecipeReference(
    @SerialName("recipeId") val recipeId: String
)

// --- Domain Models for UI use ---
data class DailyPlan(
    val user_id: String,
    val date: String,
    val meals: List<RealMealSlot>
)

data class RealMealSlot(
    val mealType: MealType,
    val recipes: List<Recipe>
)

// Enum for standard meal categories
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}

/**
 * Converts DB/DTO Map<String, List<MealSlotDTO>> into UI Domain Map<DayOfWeek, List<RealMealSlot>>
 */
fun Map<String, List<MealSlotDTO>>.toDomain(
    allRecipesMap: Map<String, Recipe> = emptyMap()
): Map<DayOfWeek, List<RealMealSlot>> {
    return this.mapEntriesNotNull { (dayString, slotDTOs) ->
        val dayOfWeek = runCatching { DayOfWeek.valueOf(dayString.uppercase()) }.getOrNull()
        if (dayOfWeek == null) null
        else dayOfWeek to slotDTOs.map { slotDto -> slotDto.toDomain(allRecipesMap) }
    }.toMap()
}

/**
 * Converts MealSlotDTO to RealMealSlot domain model
 */
fun MealSlotDTO.toDomain(allRecipesMap: Map<String, Recipe> = emptyMap()): RealMealSlot {
    return RealMealSlot(
        mealType = this.mealType,
        recipes = this.recipes.mapNotNull { ref -> allRecipesMap[ref.recipeId] }
    )
}

/**
 * Converts Domain Map<DayOfWeek, List<RealMealSlot>> back to DTO Map<String, List<MealSlotDTO>>
 */
fun Map<DayOfWeek, List<RealMealSlot>>.toDTO(): Map<String, List<MealSlotDTO>> {
    return this.mapKeys { (day, _) -> day.name }
        .mapValues { (_, slots) ->
            slots.map { slot ->
                MealSlotDTO(
                    mealType = slot.mealType,
                    recipes = slot.recipes.map { recipe -> RecipeReference(recipeId = recipe.recipe_id?:"") }
                )
            }
        }
}

// Inline helper function for safely filtering null keys during mapping
private inline fun <K, V, R, B> Map<K, V>.mapEntriesNotNull(transform: (Map.Entry<K, V>) -> Pair<R, B>?): Map<R, B> {
    val result = mutableMapOf<R, B>()
    for (entry in this) {
        val pair = transform(entry)
        if (pair != null) {
            result[pair.first] = pair.second
        }
    }
    return result
}
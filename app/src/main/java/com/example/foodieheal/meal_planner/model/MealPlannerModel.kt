package com.example.foodieheal.meal_planner.model

import com.example.foodieheal.Recipe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- DB Data Transfer Objects ---
@Serializable
data class DailyPlanDTO(
    @SerialName("user_id") val userId: String,
    val date: String,
    val meals: List<MealSlotDTO> = emptyList()
)

@Serializable
data class MealSlotDTO(
    @SerialName("mealType") val mealType: MealType,
    val recipes: List<RecipeReference> = emptyList()
)

@Serializable
data class RecipeReference(
    // 2. Ensuring camelCase matches your JSON property key: "recipeId"
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
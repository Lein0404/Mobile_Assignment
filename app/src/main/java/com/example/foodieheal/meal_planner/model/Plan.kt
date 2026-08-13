package com.example.foodieheal.meal_planner.model

import com.example.foodieheal.model.Recipe
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

/**
 * 💻 UI Model Layer: Used directly by your Composables.
 * Contains actual Recipe objects so the UI can instantly show names, images, and calories.
 */
data class WeeklyPlan(
    val planName: String = "",
    val planId: String = "",
    val userId: String = "",
    val category: PlanCategory = PlanCategory.BALANCED,
    val dailyPlans: Map<DayOfWeek, List<RealMealSlot>> = emptyMap()
)

/**
 * 📦 Remote Entity / DTO Layer: Used for Firestore or Ktor backend storage.
 * Saves String IDs to keep network payloads lightweight.
 */
@Serializable
data class WeeklyPlanEntity(
    val planName: String = "",
    val planId: String = "",
    val userId: String = "",
    val category: PlanCategory = PlanCategory.BALANCED,
    val dailyPlans: Map<String, List<MealSlotDTO>> = emptyMap() // Key: "MONDAY", Value: List of slots
)

// ==========================================
// Extension Converters
// ==========================================

// Convert UI state to clean Remote Entity Form
/**
 * Converts the populated UI domain model into lightweight entity JSON payloads for Supabase.
 */
fun WeeklyPlan.toEntity(): WeeklyPlanEntity {
    return WeeklyPlanEntity(
        planName = this.planName,
        planId = this.planId,
        userId = this.userId,
        category = this.category,
        dailyPlans = this.dailyPlans.entries.associate { (day, mealSlots) ->
            day.name to mealSlots.map { slot ->
                MealSlotDTO(
                    mealType = slot.mealType,
                    recipes = slot.recipes.mapNotNull { recipe ->
                        recipe.recipe_id?.let { RecipeReference(recipeId = it) }
                    }
                )
            }
        }
    )
}
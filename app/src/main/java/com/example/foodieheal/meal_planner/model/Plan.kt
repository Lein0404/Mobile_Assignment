package com.example.foodieheal.meal_planner.model

import com.example.foodieheal.model.Recipe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

/**
 * 💻 UI Model Layer: Used directly by your Composables.
 */
data class WeeklyPlan(
    val planName: String = "",
    val planId: String = "",
    val userId: String = "",
    val category: PlanCategory = PlanCategory.BALANCED,
    val dailyPlans: Map<DayOfWeek, List<RealMealSlot>> = emptyMap(),
    val public: Boolean = false
)

/**
 * 📦 Remote Entity / DTO Layer: Used for Supabase storage.
 */
/**
 * 📦 Remote Entity / DTO Layer: Used for Supabase storage.
 */
@Serializable
data class WeeklyPlanEntity(
    @SerialName("planId")
    val planId: String = "",

    @SerialName("planName")
    val planName: String = "",

    @SerialName("userId")
    val userId: String = "",

    val category: PlanCategory = PlanCategory.BALANCED,

    @SerialName("dailyPlans")
    val dailyPlans: Map<String, List<MealSlotDTO>> = emptyMap(),

    @SerialName("is_public")
    val public: Boolean
)

// ==========================================
// Extension Converters
// ==========================================

/**
 * Converts UI Domain model to lightweight Entity/DTO payload for Supabase.
 */
fun WeeklyPlan.toEntity(): WeeklyPlanEntity {
    return WeeklyPlanEntity(
        planId = this.planId,
        planName = this.planName,
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
        },
        public = this.public
    )
}

/**
 * 🌟 NEW: Converts Remote Entity/DTO back to UI Domain model.
 * Pass the fetched recipes map/list so the UI has full Recipe details.
 */
fun WeeklyPlanEntity.toDomain(allRecipes: List<Recipe>): WeeklyPlan {
    val domainDailyPlans = this.dailyPlans.mapKeys { (dayString, _) ->
        DayOfWeek.valueOf(dayString) // Converts "MONDAY" string to DayOfWeek.MONDAY
    }.mapValues { (_, slotDTOs) ->
        slotDTOs.map { slotDTO ->
            RealMealSlot(
                mealType = slotDTO.mealType,
                recipes = slotDTO.recipes.mapNotNull { ref ->
                    allRecipes.find { it.recipe_id == ref.recipeId }
                }
            )
        }
    }

    return WeeklyPlan(
        planName = this.planName,
        planId = this.planId,
        userId = this.userId,
        category = this.category,
        dailyPlans = domainDailyPlans,
        public = this.public
    )
}
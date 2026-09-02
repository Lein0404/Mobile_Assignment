package com.example.foodieheal.meal_planner.model

import com.example.foodieheal.Recipe.Model.Recipe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

/**
 * 💻 UI Model Layer: Used directly by your Composables.
 */
data class WeeklyPlan(
    val planName: String = "",
    val planDescription: String = "",
    val planId: String = "",
    val userId: String = "",
    val category: PlanCategory = PlanCategory.BALANCED,
    val dailyPlans: Map<DayOfWeek, List<RealMealSlot>> = emptyMap(),
    val public: Boolean = false
)

/**
 * 📦 Remote Entity / DTO Layer: Used for Supabase storage.
 */
@Serializable
data class WeeklyPlanEntity(
    @SerialName("planId")
    val planId: String = "",

    @SerialName("planName")
    val planName: String = "",

    @SerialName("planDescription")
    val planDescription: String = "",

    @SerialName("userId")
    val userId: String = "",

    @SerialName("category")
    val category: PlanCategory = PlanCategory.BALANCED,

    @SerialName("dailyPlans")
    val dailyPlans: Map<String, List<MealSlotDTO>> = emptyMap(),

    @SerialName("is_public")
    val public: Boolean = false
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
        planDescription = this.planDescription,
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
 * Pass the fetched recipes map/list so the UI has full Recipe details.
 */
fun WeeklyPlanEntity.toDomain(allRecipes: List<Recipe>): WeeklyPlan {
    val recipeMap = allRecipes.filter { it.recipe_id != null }.associateBy { it.recipe_id!! }
    return toDomain(recipeMap)
}

/**
 * Overload that takes a Map for better performance when bulk processing.
 */
fun WeeklyPlanEntity.toDomain(recipeMap: Map<String, Recipe>): WeeklyPlan {
    val domainDailyPlans = this.dailyPlans.mapKeys { (dayString, _) ->
        try {
            DayOfWeek.valueOf(dayString.uppercase())
        } catch (e: Exception) {
            DayOfWeek.MONDAY
        }
    }.mapValues { (_, slotDTOs) ->
        slotDTOs.map { slotDTO ->
            RealMealSlot(
                mealType = slotDTO.mealType,
                recipes = slotDTO.recipes.mapNotNull { ref ->
                    recipeMap[ref.recipeId]
                }
            )
        }
    }

    return WeeklyPlan(
        planName = this.planName,
        planDescription = this.planDescription,
        planId = this.planId,
        userId = this.userId,
        category = this.category,
        dailyPlans = domainDailyPlans,
        public = this.public
    )
}

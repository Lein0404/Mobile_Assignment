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
    val planIdOld: String? = null,
    @SerialName("plan_id")
    val planIdNew: String? = null,

    @SerialName("planName")
    val planNameOld: String? = null,
    @SerialName("plan_name")
    val planNameNew: String? = null,

    @SerialName("planDescription")
    val planDescriptionOld: String? = null,
    @SerialName("plan_description")
    val planDescriptionNew: String? = null,

    @SerialName("userId")
    val userIdOld: String? = null,
    @SerialName("user_id")
    val userIdNew: String? = null,

    val category: PlanCategory = PlanCategory.BALANCED,

    @SerialName("dailyPlans")
    val dailyPlansOld: Map<String, List<MealSlotDTO>>? = null,
    @SerialName("daily_plans")
    val dailyPlansNew: Map<String, List<MealSlotDTO>>? = null,

    @SerialName("is_public")
    val public: Boolean = false
) {
    val planId: String get() = planIdNew ?: planIdOld ?: ""
    val planName: String get() = planNameNew ?: planNameOld ?: ""
    val planDescription: String get() = planDescriptionNew ?: planDescriptionOld ?: ""
    val userId: String get() = userIdNew ?: userIdOld ?: ""
    val dailyPlans: Map<String, List<MealSlotDTO>> get() = dailyPlansNew ?: dailyPlansOld ?: emptyMap()
}

// ==========================================
// Extension Converters
// ==========================================

/**
 * Converts UI Domain model to lightweight Entity/DTO payload for Supabase.
 */
fun WeeklyPlan.toEntity(): WeeklyPlanEntity {
    return WeeklyPlanEntity(
        planIdNew = this.planId,
        planNameNew = this.planName,
        planDescriptionNew = this.planDescription,
        userIdNew = this.userId,
        category = this.category,
        dailyPlansNew = this.dailyPlans.entries.associate { (day, mealSlots) ->
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
    val domainDailyPlans = this.dailyPlans.mapKeys { (dayString, _) ->
        DayOfWeek.valueOf(dayString) // Converts "MONDAY" string to DayOfWeek.MONDAY
    }.mapValues { (_, slotDTOs) ->
        slotDTOs.map { slotDTO ->
            RealMealSlot(
                mealType = slotDTO.realType,
                recipes = slotDTO.recipes.mapNotNull { ref ->
                    allRecipes.find { it.recipe_id == ref.realId }
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

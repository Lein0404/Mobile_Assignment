package com.example.foodieheal.meal_planner.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.foodieheal.R

enum class PlanCategory(
    val dbKey: String,                 // 🔑 Safe key stored in database/Firestore
    @StringRes val displayNameRes: Int, // 🌐 Localized UI string reference
    @DrawableRes val iconRes: Int       // 🎨 Component vector icon reference
) {
    HIGH_PROTEIN(
        dbKey = "high_protein",
        displayNameRes = R.string.category_high_protein,
        iconRes = R.drawable.ic_high_protein
    ),
    QUICK_EASY(
        dbKey = "quick_easy",
        displayNameRes = R.string.category_quick_easy,
        iconRes = R.drawable.ic_quick_easy
    ),
    BALANCED(
        dbKey = "balanced",
        displayNameRes = R.string.category_balanced,
        iconRes = R.drawable.ic_balanced
    ),
    KETOGENIC(
        dbKey = "ketogenic",
        displayNameRes = R.string.category_ketogenic,
        iconRes = R.drawable.ic_keto
    ),
    LOW_CARB(
        dbKey = "low_carb",
        displayNameRes = R.string.category_low_carb,
        iconRes = R.drawable.ic_low_carb
    ),
    COST_FRIENDLY(
        dbKey = "cost_friendly",
        displayNameRes = R.string.category_cost_friendly,
        iconRes = R.drawable.ic_budget
    ),
    OTHERS(
        dbKey = "others",
        displayNameRes = R.string.category_others,
        iconRes = R.drawable.ic_others
    );

    companion object {
        /**
         * Converts database string back into type-safe enum.
         * Automatically falls back to OTHERS if the backend key is unmapped or null.
         */
        fun fromDbKey(key: String?): PlanCategory {
            return entries.find { it.dbKey.equals(key, ignoreCase = true) } ?: OTHERS
        }
    }
}
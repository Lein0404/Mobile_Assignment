package com.example.foodieheal.meal_planner.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.foodieheal.R

enum class PlanCategory(
    val dbKey: String,                 // 🔑 Safe key stored in database/Firestore
    @StringRes val displayNameRes: Int, // 🌐 Localized UI string reference
){
    HIGH_PROTEIN(
        dbKey = "high_protein",
        displayNameRes = R.string.category_high_protein,
    ),
    QUICK_EASY(
        dbKey = "quick_easy",
        displayNameRes = R.string.category_quick_easy,
    ),
    BALANCED(
        dbKey = "balanced",
        displayNameRes = R.string.category_balanced,
    ),
    KETOGENIC(
        dbKey = "ketogenic",
        displayNameRes = R.string.category_ketogenic,
    ),
    LOW_CARB(
        dbKey = "low_carb",
        displayNameRes = R.string.category_low_carb,
    ),
    COST_FRIENDLY(
        dbKey = "cost_friendly",
        displayNameRes = R.string.category_cost_friendly,
    ),
    OTHERS(
        dbKey = "others",
        displayNameRes = R.string.category_others
    );

    companion object {
        /**
         * Converts database string back into type-safe enum.
         * Automatically falls back to OTHERS if the backend key is unmapped or null.
         */
        fun fromDbKey(key: String?): PlanCategory {
            return entries.find { it.dbKey.equals(key, ignoreCase = true) } ?: OTHERS
        }
        val catList = listOf(
            HIGH_PROTEIN.displayNameRes,
            LOW_CARB.displayNameRes,
            QUICK_EASY.displayNameRes,
            KETOGENIC.displayNameRes,
            BALANCED.displayNameRes,
            COST_FRIENDLY.displayNameRes,
            OTHERS.displayNameRes
        )
    }
}
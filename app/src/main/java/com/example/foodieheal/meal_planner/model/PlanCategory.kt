package com.example.foodieheal.meal_planner.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.foodieheal.R

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PlanCategory(
    val dbKey: String,                 // 🔑 Safe key stored in database/Firestore
    @StringRes val displayNameRes: Int, // 🌐 Localized UI string reference
){
    @SerialName("HIGH_PROTEIN")
    HIGH_PROTEIN(
        dbKey = "HIGH_PROTEIN",
        displayNameRes = R.string.category_high_protein,
    ),
    @SerialName("QUICK_EASY")
    QUICK_EASY(
        dbKey = "QUICK_EASY",
        displayNameRes = R.string.category_quick_easy,
    ),
    @SerialName("BALANCED")
    BALANCED(
        dbKey = "BALANCED",
        displayNameRes = R.string.category_balanced,
    ),
    @SerialName("KETOGENIC")
    KETOGENIC(
        dbKey = "KETOGENIC",
        displayNameRes = R.string.category_ketogenic,
    ),
    @SerialName("LOW_CARB")
    LOW_CARB(
        dbKey = "LOW_CARB",
        displayNameRes = R.string.category_low_carb,
    ),
    @SerialName("COST_FRIENDLY")
    COST_FRIENDLY(
        dbKey = "COST_FRIENDLY",
        displayNameRes = R.string.category_cost_friendly,
    ),
    @SerialName("OTHERS")
    OTHERS(
        dbKey = "OTHERS",
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
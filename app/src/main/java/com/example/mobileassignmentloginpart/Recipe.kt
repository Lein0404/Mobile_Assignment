package com.example.mobileassignmentloginpart

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.painterResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    @SerialName("recipe_id") val recipe_id: String,
    @SerialName("recipe_name") val recipeName: String,         // Fixed: Maps to recipe_name
    @SerialName("recipe_description") val recipeDescription: String, // Fixed: Maps to recipe_description
    val budget: Double,
    @SerialName("skill_level") val skillLevel: Int,           // Fixed: Maps to skill_level
    @SerialName("prep_time") val time: Int,                    // Fixed: Maps to prep_time
    val calories: Int,
    @DrawableRes val recipeImage: Int = R.drawable.ic_launcher_background,
    @SerialName("recipe_step") val recipeStep: String          // Fixed: Maps to recipe_step
//    val category: IngCat,
//    val ingList: List<IngredientUnits>
) {
    companion object {
    }
}
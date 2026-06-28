package com.example.mobileassignmentloginpart

data class Recipe(
    val recipeId: String,
    val recipeName: String,
    val recipeDescription: String,
    val budget: Double,
    val skillLevel: Int,      // 1 = Beginner, 2 = Basic, 3 = Intermediate, 4 = Advanced, 5 = Expert
    val time: String,
    val recipeImage: String,
    val recipeStep: String,
//    val category: IngCat,
//    val ingList: List<IngredientUnits>
)
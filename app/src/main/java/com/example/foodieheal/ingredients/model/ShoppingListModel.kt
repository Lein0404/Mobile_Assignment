package com.example.foodieheal.ingredients.model

import com.example.foodieheal.ingredients.local.ShoppingListEntity

data class ShoppingListItem(
    val entity: ShoppingListEntity,
    val category: IngredientCategory?,
    val ingredientDesc: String = ""
)
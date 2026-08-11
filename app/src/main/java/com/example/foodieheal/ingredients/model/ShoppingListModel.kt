package com.example.foodieheal.ingredients.model

import com.example.foodieheal.ingredients.local.ShoppingListEntity

data class ShoppingListUiState(
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val items: List<ShoppingListItem> = emptyList(),
    val filteredItems: List<ShoppingListItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ShoppingListItem(
    val entity: ShoppingListEntity,
    val category: IngredientCategory?,
    val calorieSummary: String = ""
)
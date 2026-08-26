package com.example.foodieheal.ingredients.model

/**
 * Domain model representing a Shopping List.
 * Each shopping list has an ID (e.g. "SPL0001"), userId, list of ingredients,
 * created_at, and last_updated timestamps.
 */
data class ShoppingList(
    val shoppingListId: String = "",
    val userId: String = "",
    val title: String = "",
    val items: List<ShoppingListItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
)

/**
 * Domain model representing an ingredient inside a Shopping List.
 * Stores only ingredient name and ingredient category, plus check state.
 */
data class ShoppingListItem(
    val id: Long = 0,
    val shoppingListId: String = "",
    val ingredientId: String = "",
    val ingredientName: String = "",
    val category: IngredientCategory? = null,
    val isChecked: Boolean = false,
)
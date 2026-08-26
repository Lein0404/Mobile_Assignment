package com.example.foodieheal.ingredients.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.ShoppingList
import com.example.foodieheal.ingredients.model.ShoppingListItem

/**
 * Room entity representing a user's Shopping List metadata.
 * A user can have multiple shopping lists (identified by shopping_list_id, e.g. "SPL0001").
 */
@Entity(
    tableName = "shopping_lists",
    primaryKeys = ["shopping_list_id", "user_id"]
)
data class ShoppingListEntity(
    @ColumnInfo(name = "shopping_list_id") val shoppingListId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis(),
)

/**
 * Room entity representing an ingredient item in a Shopping List.
 * Stores only ingredient name and ingredient category (no units, descriptions, etc.).
 */
@Entity(
    tableName = "shopping_list_items",
    indices = [Index(value = ["shopping_list_id", "user_id"])]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "shopping_list_id") val shoppingListId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "ingredient_id") val ingredientId: String = "",
    @ColumnInfo(name = "ingredient_name") val ingredientName: String = "",
    @ColumnInfo(name = "ingredient_category") val ingredientCategory: String? = null,
    @ColumnInfo(name = "is_checked") val isChecked: Boolean = false,
)

/**
 * Room relation linking a ShoppingListEntity with its ShoppingListItemEntity rows.
 */
data class ShoppingListWithItemsEntity(
    @Embedded val shoppingList: ShoppingListEntity,
    @Relation(
        parentColumn = "shopping_list_id",
        entityColumn = "shopping_list_id"
    )
    val items: List<ShoppingListItemEntity>
)

// ──────────────── Converter helpers: Entity ↔ Domain Model ────────────────

fun ShoppingListItemEntity.toDomain() = ShoppingListItem(
    id = id,
    shoppingListId = shoppingListId,
    ingredientId = ingredientId,
    ingredientName = ingredientName,
    category = ingredientCategory?.let {
        try {
            IngredientCategory.valueOf(it)
        } catch (_: Exception) {
            IngredientCategory.OTHERS
        }
    },
    isChecked = isChecked
)

fun ShoppingListItem.toEntity(userId: String) = ShoppingListItemEntity(
    id = id,
    shoppingListId = shoppingListId,
    userId = userId,
    ingredientId = ingredientId,
    ingredientName = ingredientName,
    ingredientCategory = category?.name,
    isChecked = isChecked
)

fun ShoppingListWithItemsEntity.toDomain() = ShoppingList(
    shoppingListId = shoppingList.shoppingListId,
    userId = shoppingList.userId,
    title = shoppingList.title,
    isDefault = shoppingList.isDefault,
    items = items.map { it.toDomain() },
    createdAt = shoppingList.createdAt,
    lastUpdated = shoppingList.lastUpdated
)

fun ShoppingListEntity.toDomain(items: List<ShoppingListItem> = emptyList()) = ShoppingList(
    shoppingListId = shoppingListId,
    userId = userId,
    title = title,
    isDefault = isDefault,
    items = items,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)

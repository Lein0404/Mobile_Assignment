package com.example.foodieheal.ingredients.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the local Shopping List.
 * This table starts empty — users populate it by adding ingredients.
 * It is local-only and does not sync to Supabase.
 */
@Entity(tableName = "shopping_list")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "ingredient_id") val ingredientId: String,
    @ColumnInfo(name = "ingredient_name") val ingredientName: String = "",
    @ColumnInfo(name = "quantity") val quantity: Double = 1.0,
    @ColumnInfo(name = "unit_id") val unitId: String? = null,
    @ColumnInfo(name = "unit_name") val unitName: String? = null,
    @ColumnInfo(name = "is_checked") val isChecked: Boolean = false,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
)

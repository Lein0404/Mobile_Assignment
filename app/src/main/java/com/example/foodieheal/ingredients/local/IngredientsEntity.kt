package com.example.foodieheal.ingredients.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.IngredientUnits
import com.example.foodieheal.ingredients.model.Ingredients
import com.example.foodieheal.ingredients.model.Units

/**
 * Room entity for the local database mirroring the Supabase `ingredients` table.
 */
@Entity(tableName = "ingredients")
data class IngredientsEntity(
    @PrimaryKey
    @ColumnInfo(name = "ingredient_id") val ingredientId: String,
    @ColumnInfo(name = "ing_name") val ingredientName: String = "",
    @ColumnInfo(name = "ing_category") val ingredientCategory: String? = null,
    @ColumnInfo(name = "ing_description") val ingredientDesc: String = "",
    @ColumnInfo(name = "ing_image") val ingredientImage: String? = null,
    @ColumnInfo(name = "created_by_user_id") val createdByUserId: String? = null,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "ing_alt_names") val ingredientAltNames: String = "",
)

/**
 * Room entity for the local database mirroring the Supabase `ingredient_units` table.
 */
@Entity(tableName = "ingredient_units")
data class IngredientUnitsEntity(
    @PrimaryKey
    @ColumnInfo(name = "ingredient_unit_id") val ingredientUnitId: String,
    @ColumnInfo(name = "ingredient_id") val ingredientId: String = "",
    @ColumnInfo(name = "unit_id") val unitId: String = "",
    @ColumnInfo(name = "calories_per_default_quantity") val caloriesPerDefaultQuantity: Double = 0.0,
)

/**
 * Room entity for the local database mirroring the Supabase `units` table.
 */
@Entity(tableName = "units")
data class UnitsEntity(
    @PrimaryKey
    @ColumnInfo(name = "unit_id") val unitId: String,
    @ColumnInfo(name = "unit_name") val unitName: String = "",
    @ColumnInfo(name = "unit_display") val unitDisplay: String = "",
    @ColumnInfo(name = "default_quantity") val defaultQuantity: Double = 1.0,
)

// ──────────────── Converter helpers: Entity ↔ Domain Model ────────────────

fun Ingredients.toEntity() = IngredientsEntity(
    ingredientId = ingredientId,
    ingredientName = ingredientName,
    ingredientCategory = ingredientCategory?.name,
    ingredientDesc = ingredientDesc,
    ingredientImage = ingredientImage,
    createdByUserId = createdByUserId,
    isDefault = isDefault,
    ingredientAltNames = ingredientAltNames.joinToString("|||"),
)

fun IngredientsEntity.toDomain() = Ingredients(
    ingredientId = ingredientId,
    ingredientName = ingredientName,
    ingredientCategory = ingredientCategory?.let {
        try { IngredientCategory.valueOf(it) } catch (_: Exception) { null }
    },
    ingredientDesc = ingredientDesc,
    ingredientImage = ingredientImage,
    createdByUserId = createdByUserId,
    isDefault = isDefault,
    ingredientAltNames = if (ingredientAltNames.isBlank()) emptyList() else ingredientAltNames.split("|||"),
)

fun IngredientUnits.toEntity() = IngredientUnitsEntity(
    ingredientUnitId = ingredientUnitId,
    ingredientId = ingredientID,
    unitId = unitID,
    caloriesPerDefaultQuantity = caloriesPerDefaultQuantity,
)

fun IngredientUnitsEntity.toDomain() = IngredientUnits(
    ingredientUnitId = ingredientUnitId,
    ingredientID = ingredientId,
    unitID = unitId,
    caloriesPerDefaultQuantity = caloriesPerDefaultQuantity,
)

fun Units.toEntity() = UnitsEntity(
    unitId = unitID,
    unitName = unitName,
    unitDisplay = unitDisplay,
    defaultQuantity = defaultQuantity,
)

fun UnitsEntity.toDomain() = Units(
    unitID = unitId,
    unitName = unitName,
    unitDisplay = unitDisplay,
    defaultQuantity = defaultQuantity,
)

package com.example.foodieheal.ingredients.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (DAO) for the local ingredients cache (ingredients, ingredient_units, units tables).
 * Provides CRUD operations for the offline cache that mirrors Supabase data.
 * 
 * Key Functions:
 * - @Dao: Marks this interface as a DAO for Room.
 * - @Insert: Inserts data into the database.
 * - @Query: Executes SQL queries to retrieve or delete data.
 * - @Upsert: Insert or update operation.
 * - Flow<T>: Returns reactive streams for real-time UI updates.
 * - suspend functions: For database operations that run on background threads.
 */
@Dao
interface IngredientsDao {

    // ──────── Ingredients ────────

    @Query("SELECT * FROM ingredients")
    suspend fun getAllIngredients(): List<IngredientsEntity>

    @Query("SELECT * FROM ingredients WHERE ingredient_id = :id")
    suspend fun getIngredientById(id: String): IngredientsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIngredients(ingredients: List<IngredientsEntity>)

    @Query("DELETE FROM ingredients")
    suspend fun clearIngredients()

    // ──────── Ingredient Units ────────

    @Query("SELECT * FROM ingredient_units")
    suspend fun getAllIngredientUnits(): List<IngredientUnitsEntity>

    @Query("SELECT * FROM ingredient_units WHERE ingredient_id = :ingredientId")
    suspend fun getIngredientUnitsById(ingredientId: String): List<IngredientUnitsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIngredientUnits(ingredientUnits: List<IngredientUnitsEntity>)

    @Query("DELETE FROM ingredient_units")
    suspend fun clearIngredientUnits()

    // ──────── Units ────────

    @Query("SELECT * FROM units")
    suspend fun getAllUnits(): List<UnitsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUnits(units: List<UnitsEntity>)

    @Query("DELETE FROM units")
    suspend fun clearUnits()
}

package com.example.foodieheal.ingredients.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database containing the local ingredients cache and shopping list.
 *
 * - Ingredients cache (3 tables): mirrors Supabase for offline access
 * - Shopping list (1 table): local-only, user's personal shopping list
 * 
 * Key Features:
 * - @Database: Marks this as a Room database.
 * - entities: Specifies the entities (tables) that belong to this database.
 * - version: The version number of the database. (increment when changing schema / structure)
 * - exportSchema: Whether to export the schema.
 */
@Database(
    entities = [
        IngredientsEntity::class,
        IngredientUnitsEntity::class,
        UnitsEntity::class,
        ShoppingListEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class IngredientsDatabase : RoomDatabase() {

    /**
     * @return ingredientsDao instance to access the ingredients-related tables (ingredients, ingredient_units, units).
     */
    abstract fun ingredientsDao(): IngredientsDao
    /**
     * @return shoppingListDao instance to access the shopping list table.
     */
    abstract fun shoppingListDao(): ShoppingListDao

    /**
     * Singleton instance of the database
     */
    companion object {
        @Volatile
        private var INSTANCE: IngredientsDatabase? = null

            fun getInstance(context: Context): IngredientsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IngredientsDatabase::class.java,
                    "ingredients_database"
                )
                    // TODO?
                    .fallbackToDestructiveMigration() // if database schema changes, the database will be recreated from scratch
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

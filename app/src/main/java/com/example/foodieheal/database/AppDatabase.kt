package com.example.foodieheal.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 🌟 Bumped version to 2 to handle new tables safely
@Database(entities = [UserEntity::class, ChefEntity::class, RecipeEntity::class, IngredientEntity::class, RecipeBookmarkEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "foodieheal_database"
                )
                .fallbackToDestructiveMigration() // Wipes old data if version changes to prevent crashes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

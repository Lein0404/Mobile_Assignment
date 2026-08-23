package com.example.foodieheal.hiring.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChefEntity::class,
        AppointmentEntity::class,
        ChefBookmarkEntity::class,
        ChefReviewEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HiringDatabase : RoomDatabase() {

    abstract fun chefDao(): ChefDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun chefBookmarkDao(): ChefBookmarkDao
    abstract fun chefReviewDao(): ChefReviewDao

    companion object {
        @Volatile
        private var INSTANCE: HiringDatabase? = null

        fun getInstance(context: Context): HiringDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HiringDatabase::class.java,
                    "hiring_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

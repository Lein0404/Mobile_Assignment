package com.example.foodieheal.Chef.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChefPortalAppointmentEntity::class,
        ChefPortalUserEntity::class,
        ChefProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ChefDatabase : RoomDatabase() {

    abstract fun chefPortalAppointmentDao(): ChefPortalAppointmentDao
    abstract fun chefPortalUserDao(): ChefPortalUserDao
    abstract fun chefProfileDao(): ChefProfileDao

    companion object {
        @Volatile
        private var INSTANCE: ChefDatabase? = null

        fun getInstance(context: Context): ChefDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChefDatabase::class.java,
                    "chef_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

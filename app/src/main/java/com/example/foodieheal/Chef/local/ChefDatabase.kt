package com.example.foodieheal.Chef.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ChefPortalAppointmentEntity::class,
        ChefPortalUserEntity::class,
        ChefProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ChefDatabase : RoomDatabase() {

    abstract fun chefPortalAppointmentDao(): ChefPortalAppointmentDao
    abstract fun chefPortalUserDao(): ChefPortalUserDao
    abstract fun chefProfileDao(): ChefProfileDao

    companion object {
        const val SENTINEL_FILE_NAME = "chef_cache_sentinel.marker"

        @Volatile
        private var INSTANCE: ChefDatabase? = null

        fun getInstance(context: Context): ChefDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChefDatabase::class.java,
                    "chef_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }

                // Check if user cleared the phone cache memory
                checkAndPurgeCacheIfCleared(context.applicationContext, db)

                db
            }
        }

        fun checkAndPurgeCacheIfCleared(context: Context, database: ChefDatabase? = null) {
            val sentinelFile = File(context.cacheDir, SENTINEL_FILE_NAME)
            if (!sentinelFile.exists()) {
                val db = database ?: INSTANCE
                if (db != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            db.chefPortalAppointmentDao().clearAllAppointments()
                            db.chefPortalUserDao().clearAllUsers()
                            sentinelFile.createNewFile()
                        } catch (e: Exception) {
                            android.util.Log.e("ChefDatabase", "Failed to clear appointment cache after phone cache wipe", e)
                        }
                    }
                }
            }
        }
    }
}

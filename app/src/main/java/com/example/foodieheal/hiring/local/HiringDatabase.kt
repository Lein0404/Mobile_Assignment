package com.example.foodieheal.hiring.local

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
        ChefEntity::class,
        AppointmentEntity::class,
        ChefBookmarkEntity::class,
        ChefReviewEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HiringDatabase : RoomDatabase() {

    abstract fun chefDao(): ChefDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun chefBookmarkDao(): ChefBookmarkDao
    abstract fun chefReviewDao(): ChefReviewDao

    companion object {
        const val SENTINEL_FILE_NAME = "hiring_cache_sentinel.marker"

        @Volatile
        private var INSTANCE: HiringDatabase? = null

        fun getInstance(context: Context): HiringDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HiringDatabase::class.java,
                    "hiring_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }

                // Check if user clear the phone cache memory
                checkAndPurgeCacheIfCleared(context.applicationContext, db)

                db
            }
        }

        fun checkAndPurgeCacheIfCleared(context: Context, database: HiringDatabase? = null) {
            val sentinelFile = File(context.cacheDir, SENTINEL_FILE_NAME)
            if (!sentinelFile.exists()) {
                val db = database ?: INSTANCE
                if (db != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            db.chefDao().clearChefs()
                            db.appointmentDao().clearAppointments()
                            db.chefBookmarkDao().clearAllBookmarks()
                            db.chefReviewDao().clearAllReviews()
                            sentinelFile.createNewFile()
                        } catch (e: Exception) {
                            android.util.Log.e("HiringDatabase", "Failed to clear hiring cache after phone cache wipe", e)
                        }
                    }
                }
            }
        }
    }
}

package com.example.foodieheal.Payment.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PaymentMethodRoomEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PayMethodDatabase : RoomDatabase() {

    abstract fun paymentMethodDao(): PaymentMethodDao

    companion object {
        @Volatile
        private var INSTANCE: PayMethodDatabase? = null

        fun getDatabase(context: Context): PayMethodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PayMethodDatabase::class.java,
                    "payment_method_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
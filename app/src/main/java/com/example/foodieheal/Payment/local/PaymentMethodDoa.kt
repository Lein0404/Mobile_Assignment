package com.example.foodieheal.Payment.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodieheal.Payment.data.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {

    @Query("SELECT * FROM payment_methods WHERE userId = :userId")
    fun getPaymentMethods(userId: String): Flow<List<PaymentMethodRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethods(methods: List<PaymentMethodRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(method: PaymentMethodRoomEntity)

    @Query("DELETE FROM payment_methods WHERE paymentMethodId = :methodId")
    suspend fun deleteById(methodId: String)

    @Delete
    suspend fun delete(paymentMethod: PaymentMethodRoomEntity)

    @Query("DELETE FROM payment_methods WHERE userId = :userId")
    suspend fun clearUserPaymentMethods(userId: String)
}
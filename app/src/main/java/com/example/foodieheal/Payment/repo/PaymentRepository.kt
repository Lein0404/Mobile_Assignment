package com.example.foodieheal.Payment.repo

import com.example.foodieheal.Payment.ViewModel.PaymentMethod
import com.example.foodieheal.Payment.data.PaymentMethodEntity
import com.example.foodieheal.Payment.local.PaymentMethodDao
import com.example.foodieheal.Payment.local.toRoomEntity
import com.example.foodieheal.Payment.local.toUiModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale.filter
import java.util.UUID

class PaymentRepository(
    private val dao: PaymentMethodDao,
    private val supabaseClient: SupabaseClient
) {
    // Read cached cards from Room database
    fun getSavedCards(userId: String): Flow<List<PaymentMethod.CreditCard>> {
        return dao.getPaymentMethods(userId).map { roomList ->
            roomList.map { it.toUiModel() }
        }
    }

    // Refresh data from Supabase and cache into Room
    suspend fun refreshPaymentMethodsFromNetwork(userId: String) {
        val remoteList = supabaseClient.from("payment_method")
            .select { filter { eq("user_id", userId) } }
            .decodeList<PaymentMethodEntity>()

        val roomEntities = remoteList.map { it.toRoomEntity() }

        dao.clearUserPaymentMethods(userId)
        dao.insertPaymentMethods(roomEntities)
    }

    // Save a new card to Supabase first, then update Room cache
    suspend fun addNewCard(
        userId: String,
        last4Digits: String,
        brand: String,
        expiryDate: String?,
        isDefault: Boolean
    ) {
        val newCardEntity = PaymentMethodEntity(
            paymentMethodId = UUID.randomUUID().toString(),
            userId = userId,
            type = "CreditCard",
            cardBrand = brand,
            last4Digits = last4Digits,
            expiryDate = expiryDate,
            isDefault = isDefault
        )

        // Save remote
        supabaseClient.from("payment_method").insert(newCardEntity)

        //Save local
        dao.insertPaymentMethod(newCardEntity.toRoomEntity())
    }

    // Delete payment method
    suspend fun deletePaymentMethod(methodId: String) = withContext(Dispatchers.IO) {
        // 1. Delete from remote Supabase database
        supabaseClient.from("payment_methods").delete {
            filter {
                eq("id", methodId)
            }
        }

        // Delete from local Room cache
        dao.deleteById(methodId)
    }
}
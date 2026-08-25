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
    ) = withContext(Dispatchers.IO) {
        if (isDefault) {
            // Reset other default cards on remote & local
            try {
                supabaseClient.from("payment_method").update({
                    set("is_default", false)
                }) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            } catch (_: Exception) {}
            dao.resetDefaultsForUser(userId)
        }

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

        // Save local
        dao.insertPaymentMethod(newCardEntity.toRoomEntity())
    }

    // Toggle / set default payment method
    suspend fun setDefaultPaymentMethod(
        userId: String,
        methodId: String,
        isDefault: Boolean
    ) = withContext(Dispatchers.IO) {
        if (isDefault) {
            // 1. Reset all cards for this user to is_default = false on Supabase
            supabaseClient.from("payment_method").update({
                set("is_default", false)
            }) {
                filter {
                    eq("user_id", userId)
                }
            }
            // 2. Set this specific card to is_default = true
            supabaseClient.from("payment_method").update({
                set("is_default", true)
            }) {
                filter {
                    eq("payment_method_id", methodId)
                }
            }
            // 3. Update local Room database
            dao.resetDefaultsForUser(userId)
            dao.updateDefault(methodId, true)
        } else {
            // If toggling off
            supabaseClient.from("payment_method").update({
                set("is_default", false)
            }) {
                filter {
                    eq("payment_method_id", methodId)
                }
            }
            dao.updateDefault(methodId, false)
        }
    }

    // Delete payment method
    suspend fun deletePaymentMethod(methodId: String) = withContext(Dispatchers.IO) {
        // 1. Delete from remote Supabase database
        supabaseClient.from("payment_method").delete {
            filter {
                eq("payment_method_id", methodId)
            }
        }

        // Delete from local Room cache
        dao.deleteById(methodId)
    }
}
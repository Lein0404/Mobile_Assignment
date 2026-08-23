package com.example.foodieheal.Payment.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodieheal.Payment.ViewModel.PaymentMethod
import com.example.foodieheal.Payment.data.PaymentMethodEntity

@Entity(tableName = "payment_methods")
data class PaymentMethodRoomEntity(
    @PrimaryKey val paymentMethodId: String,
    val userId: String,
    val type: String,
    val cardBrand: String? = null,
    val last4Digits: String? = null,
    val expiryDate: String? = null,
    val isDefault: Boolean = false,
    val createdAt: String? = null
)

// Mappers
fun PaymentMethodEntity.toRoomEntity() = PaymentMethodRoomEntity(
    paymentMethodId = paymentMethodId,
    userId = userId,
    type = type,
    cardBrand = cardBrand,
    last4Digits = last4Digits,
    expiryDate = expiryDate,
    isDefault = isDefault,
    createdAt = createdAt
)

fun PaymentMethodRoomEntity.toUiModel() = PaymentMethod.CreditCard(
    id = paymentMethodId,
    last4Digits = last4Digits.orEmpty(),
    cardBrand = cardBrand ?: "Card",
    expiryDate = expiryDate,
    isDefault = isDefault
)
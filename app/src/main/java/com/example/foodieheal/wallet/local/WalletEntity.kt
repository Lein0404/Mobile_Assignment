package com.example.foodieheal.wallet.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodieheal.wallet.model.PaymentMethodSummary
import com.example.foodieheal.wallet.model.Wallet
import com.example.foodieheal.wallet.model.WalletTransaction

@Entity(tableName = "wallets")
data class WalletRoomEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val balance: Double,
    val isActive: Boolean,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionRoomEntity(
    @PrimaryKey val id: String,
    val walletId: String,
    val paymentId: String? = null,
    val paymentMethodId: String? = null,
    val transactionType: String,
    val amount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val description: String? = null,
    val createdAt: String? = null,
    val cardBrand: String? = null,
    val last4Digits: String? = null,
    val cardType: String? = null
)

// ==========================================
// Mappers: Wallet <-> WalletRoomEntity
// ==========================================

fun Wallet.toRoomEntity(): WalletRoomEntity = WalletRoomEntity(
    id = id,
    userId = userId,
    balance = balance ?: 0.0,
    isActive = isActive == true,
    createdAt = createdAt,
    updatedAt = updateAt
)

fun WalletRoomEntity.toDomainModel(): Wallet = Wallet(
    id = id,
    userId = userId,
    balance = balance,
    isActive = isActive,
    createdAt = createdAt,
    updateAt = updatedAt
)

// ==========================================
// Mappers: WalletTransaction <-> WalletTransactionRoomEntity
// ==========================================

fun WalletTransaction.toRoomEntity(): WalletTransactionRoomEntity = WalletTransactionRoomEntity(
    id = id,
    walletId = walletId,
    paymentId = paymentId,
    paymentMethodId = paymentMethodId,
    transactionType = transactionType,
    amount = safeAmount,
    balanceBefore = safeBalanceBefore,
    balanceAfter = safeBalanceAfter,
    description = description,
    createdAt = createdAt,
    cardBrand = paymentMethod?.cardBrand,
    last4Digits = paymentMethod?.last4Digits,
    cardType = paymentMethod?.type
)

fun WalletTransactionRoomEntity.toDomainModel(): WalletTransaction = WalletTransaction(
    id = id,
    walletId = walletId,
    paymentId = paymentId,
    paymentMethodId = paymentMethodId,
    transactionType = transactionType,
    amount = amount,
    balanceBefore = balanceBefore,
    balanceAfter = balanceAfter,
    description = description,
    createdAt = createdAt,
    paymentMethod = if (!cardBrand.isNullOrBlank() || !last4Digits.isNullOrBlank() || !cardType.isNullOrBlank()) {
        PaymentMethodSummary(
            cardBrand = cardBrand,
            last4Digits = last4Digits,
            type = cardType
        )
    } else null
)

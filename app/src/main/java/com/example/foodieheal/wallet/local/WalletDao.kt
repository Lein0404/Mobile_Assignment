package com.example.foodieheal.wallet.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    fun getWalletFlow(userId: String): Flow<WalletRoomEntity?>

    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    suspend fun getWalletDirect(userId: String): WalletRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletRoomEntity)

    @Query("SELECT * FROM wallet_transactions WHERE walletId = :walletId ORDER BY createdAt DESC")
    fun getTransactionsFlow(walletId: String): Flow<List<WalletTransactionRoomEntity>>

    @Query("SELECT * FROM wallet_transactions WHERE walletId = :walletId ORDER BY createdAt DESC")
    suspend fun getTransactionsDirect(walletId: String): List<WalletTransactionRoomEntity>

    @Query("SELECT * FROM wallet_transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): WalletTransactionRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<WalletTransactionRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionRoomEntity)

    @Query("DELETE FROM wallets WHERE userId = :userId")
    suspend fun clearWalletForUser(userId: String)

    @Query("DELETE FROM wallet_transactions WHERE walletId = :walletId")
    suspend fun clearTransactionsForWallet(walletId: String)
}

package com.example.foodieheal.wallet.viewmodel

import com.example.foodieheal.wallet.model.Wallet
import com.example.foodieheal.wallet.model.WalletTransaction
import com.example.foodieheal.wallet.model.WalletTransactionType

enum class TransactionFilterOption(val label: String) {
    ALL("All"),
    TOP_UP("Top Up"),
    PAYMENT("Payments"),
    REFUND("Refunds")
}

data class WalletUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmittingTopUp: Boolean = false,
    val wallet: Wallet? = null,
    val transactions: List<WalletTransaction> = emptyList(),
    val isBalanceHidden: Boolean = false,
    val selectedFilter: TransactionFilterOption = TransactionFilterOption.ALL,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredTransactions: List<WalletTransaction>
        get() = when (selectedFilter) {
            TransactionFilterOption.ALL -> transactions
            TransactionFilterOption.TOP_UP -> transactions.filter { it.typeEnum == WalletTransactionType.TOP_UP }
            TransactionFilterOption.PAYMENT -> transactions.filter { it.typeEnum == WalletTransactionType.APPOINTMENT_PAYMENT }
            TransactionFilterOption.REFUND -> transactions.filter { it.typeEnum == WalletTransactionType.REFUND || it.typeEnum == WalletTransactionType.RESCHEDULE_ADJUSTMENT }
        }

    val currentBalance: Double
        get() = wallet?.balance ?: 0.0

    val isWalletActive: Boolean
        get() = wallet?.isActive == true
}

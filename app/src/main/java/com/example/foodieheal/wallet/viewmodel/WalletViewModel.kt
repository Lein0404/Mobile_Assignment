package com.example.foodieheal.wallet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.wallet.data.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WalletViewModel(
    private val repository: WalletRepository = WalletRepository(),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var activeUserId: String? = null

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            networkMonitor.isConnected.collectLatest { connected ->
                _isNetworkAvailable.value = connected
                if (connected && !activeUserId.isNullOrBlank()) {
                    Log.d("WalletViewModel", "Reconnected to internet. Refreshing wallet data.")
                    loadWalletData(activeUserId, isRefresh = true)
                }
            }
        }
    }

    fun initialize(userId: String?) {
        val targetId = userId?.ifBlank { null } ?: repository.getCurrentUserId()
        if (targetId != null && targetId != activeUserId) {
            activeUserId = targetId
            loadWalletData(targetId)
        } else if (targetId != null && _uiState.value.wallet == null) {
            loadWalletData(targetId)
        }
    }

    fun loadWalletData(userId: String? = activeUserId, isRefresh: Boolean = false) {
        val targetUserId = userId?.ifBlank { null } ?: repository.getCurrentUserId() ?: return
        activeUserId = targetUserId

        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                val wallet = repository.getWallet(targetUserId)
                val txns = if (wallet.id.isNotBlank()) repository.getTransactions(wallet.id) else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        wallet = wallet,
                        transactions = txns,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Failed to load wallet information."
                    )
                }
            }
        }
    }

    fun toggleBalanceVisibility() {
        _uiState.update { it.copy(isBalanceHidden = !it.isBalanceHidden) }
    }

    fun setFilter(filter: TransactionFilterOption) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun topUp(
        amount: Double,
        paymentMethodId: String? = null,
        description: String = "Wallet Top Up",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!_isNetworkAvailable.value) {
            val err = "No internet connection. Please check your network to top up your wallet."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        val targetUserId = activeUserId ?: repository.getCurrentUserId()
        if (targetUserId.isNullOrBlank()) {
            val err = "User session expired. Please log in again."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        if (amount <= 0.0) {
            val err = "Please enter a valid top-up amount."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingTopUp = true, errorMessage = null) }

            val result = repository.topUpWallet(
                userId = targetUserId,
                amount = amount,
                paymentMethodId = paymentMethodId,
                description = description
            )

            result.onSuccess { updatedWallet ->
                val txns = repository.getTransactions(updatedWallet.id)
                _uiState.update {
                    it.copy(
                        isSubmittingTopUp = false,
                        wallet = updatedWallet,
                        transactions = txns,
                        successMessage = "Top-up of RM ${String.format("%.2f", amount)} was successful!"
                    )
                }
                onSuccess()
            }.onFailure { ex ->
                val errMsg = ex.localizedMessage ?: "Top-up failed. Please try again."
                _uiState.update {
                    it.copy(
                        isSubmittingTopUp = false,
                        errorMessage = errMsg
                    )
                }
                onError(errMsg)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    class Factory(
        private val repository: WalletRepository,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
                return WalletViewModel(repository, networkMonitor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

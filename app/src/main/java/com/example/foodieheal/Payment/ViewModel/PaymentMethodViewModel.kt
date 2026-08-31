package com.example.foodieheal.Payment.ViewModel

import android.util.Log
import com.example.foodieheal.R
import com.example.foodieheal.MainActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Payment.repo.PaymentRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentMethodViewModel(
    private val repository: PaymentRepository,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private fun resString(resId: Int, vararg args: Any): String? {
        return MainActivity.appContext?.getString(resId, *args)
    }

    private val _uiState = MutableStateFlow(PaymentMethodUiState())
    val uiState: StateFlow<PaymentMethodUiState> = _uiState.asStateFlow()

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
                    Log.d("PaymentMethodViewModel", "Reconnected to internet. Refreshing payment methods.")
                    repository.refreshPaymentMethodsFromNetwork(activeUserId!!)
                }
            }
        }
    }

    fun observeAndFetchPaymentMethods(userId: String) {
        if (userId.isBlank()) return
        activeUserId = userId

        // Observe local Room database for saved cards
        viewModelScope.launch {
            repository.getSavedCards(userId).collectLatest { savedCards ->
                val defaultMethod = savedCards.firstOrNull { it.isDefault } ?: savedCards.firstOrNull()
                _uiState.update { currentState ->
                    val selected = currentState.selectedMethod
                    val isCurrentSelectionInList = savedCards.any { it.id == selected?.id }

                    val newSelected = if (selected == null || !isCurrentSelectionInList) {
                        defaultMethod
                    } else {
                        val currentCardUpdated = savedCards.firstOrNull { it.id == selected.id }
                        val hasExplicitDefault = savedCards.any { it.isDefault }
                        if (hasExplicitDefault && currentCardUpdated?.isDefault != true) {
                            defaultMethod
                        } else {
                            currentCardUpdated ?: defaultMethod
                        }
                    }

                    currentState.copy(
                        availableMethods = savedCards,
                        selectedMethod = newSelected
                    )
                }
            }
        }

        // Sync fresh card data from Supabase if online
        viewModelScope.launch {
            if (!_isNetworkAvailable.value) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.refreshPaymentMethodsFromNetwork(userId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e("PaymentMethodViewModel", "Network sync failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = resString(R.string.msg_payment_cached_cards_sync_failed) ?: "Showing cached cards. Failed to sync online."
                    )
                }
            }
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun addNewCard(
        userId: String,
        last4Digits: String,
        brand: String,
        expiryDate: String? = null,
        isDefault: Boolean = false,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!_isNetworkAvailable.value) {
            val errorMsg = resString(R.string.error_payment_no_internet_short) ?: "No internet connection. Please check your network."
            _uiState.update { it.copy(errorMessage = errorMsg) }
            onError(errorMsg)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.addNewCard(userId, last4Digits, brand, expiryDate, isDefault)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: (resString(R.string.error_payment_save_card_failed) ?: "Failed to save card.")
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                onError(errorMsg)
            }
        }
    }

    fun deletePaymentMethod(
        methodId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!_isNetworkAvailable.value) {
            val errorMsg = resString(R.string.error_payment_no_internet_short) ?: "No internet connection. Please check your network."
            _uiState.update { it.copy(errorMessage = errorMsg) }
            onError(errorMsg)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Perform deletion in repository
                repository.deletePaymentMethod(methodId)

                // Refresh payment methods list for current user
                observeAndFetchPaymentMethods(userId)

                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: (resString(R.string.error_payment_delete_method_failed) ?: "Failed to delete payment method.")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = errorMsg)
                }
                onError(errorMsg)
            }
        }
    }

    fun setDefaultPaymentMethod(
        methodId: String,
        userId: String,
        isDefault: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!_isNetworkAvailable.value) {
            val errorMsg = resString(R.string.error_payment_no_internet_short) ?: "No internet connection. Please check your network."
            _uiState.update { it.copy(errorMessage = errorMsg) }
            onError(errorMsg)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.setDefaultPaymentMethod(userId = userId, methodId = methodId, isDefault = isDefault)
                // Refresh data from network to ensure full consistency
                repository.refreshPaymentMethodsFromNetwork(userId)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                Log.e("PaymentMethodViewModel", "Failed to set default payment method", e)
                val errorMsg = e.localizedMessage ?: (resString(R.string.error_payment_set_default_failed) ?: "Failed to set default payment method.")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = errorMsg)
                }
                onError(errorMsg)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Factory required because ViewModel takes a repository argument in constructor
    class Factory(
        private val repository: PaymentRepository,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentMethodViewModel::class.java)) {
                return PaymentMethodViewModel(repository, networkMonitor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
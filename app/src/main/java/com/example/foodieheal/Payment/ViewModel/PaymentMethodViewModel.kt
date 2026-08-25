package com.example.foodieheal.Payment.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Payment.data.PaymentMethodEntity
import com.example.foodieheal.Payment.repo.PaymentRepository
import com.example.foodieheal.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class PaymentMethodViewModel(
    private val repository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentMethodUiState())
    val uiState: StateFlow<PaymentMethodUiState> = _uiState.asStateFlow()

    fun observeAndFetchPaymentMethods(userId: String) {
        if (userId.isBlank()) return

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

        // Sync fresh card data from Supabase
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.refreshPaymentMethodsFromNetwork(userId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e("PaymentMethodViewModel", "Network sync failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Showing cached cards. Failed to sync online."
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.addNewCard(userId, last4Digits, brand, expiryDate, isDefault)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Failed to save card."
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
                val errorMsg = e.localizedMessage ?: "Failed to delete payment method."
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
                val errorMsg = e.localizedMessage ?: "Failed to set default payment method."
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
    class Factory(private val repository: PaymentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentMethodViewModel::class.java)) {
                return PaymentMethodViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
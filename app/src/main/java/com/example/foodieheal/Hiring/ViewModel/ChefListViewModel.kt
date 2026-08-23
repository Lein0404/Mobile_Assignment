package com.example.foodieheal.hiring.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.mobileassignmentloginpart.Model.Chef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ChefListViewModel(
    private val repository: HiringRepository = HiringRepository()
) : ViewModel() {

    private val _chefList = MutableStateFlow<List<Chef>>(emptyList())
    val chefList: StateFlow<List<Chef>> = _chefList.asStateFlow()

    private val _selectedChef = MutableStateFlow<Chef?>(null)
    val selectedChef: StateFlow<Chef?> = _selectedChef.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun selectChef(chef: Chef) {
        _selectedChef.value = chef
    }

    fun clearSelectedChef() {
        _selectedChef.value = null
    }

    fun fetchAllChefs() {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null

            try {
                _chefList.value = repository.fetchAllChefs()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ChefListViewModel", "Error fetching chefs list", e)
                _errorMessage.value = e.message ?: "Failed to fetch chef profiles"
            } finally {
                _isProcessing.value = false
            }
        }
    }
}

// Backward compatibility alias
typealias HiringViewModel = ChefListViewModel

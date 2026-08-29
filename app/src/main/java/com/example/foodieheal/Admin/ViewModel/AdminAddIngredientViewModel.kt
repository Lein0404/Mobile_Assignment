package com.example.foodieheal.Admin.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.shared.IngredientFormHelper
import com.example.foodieheal.ingredients.shared.IngredientFormState
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.foodieheal.ingredients.repo.IngredientRequestRepository

data class AdminAddIngredientUiState(
    val isNetworkAvailable: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: Int? = null
)

class AdminAddIngredientViewModel(
    application: Application,
    private val repository: IngredientsRepository,
    private val requestRepository: IngredientRequestRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AdminAddIngredientUiState())
    val uiState: StateFlow<AdminAddIngredientUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(IngredientFormState())
    val formState: StateFlow<IngredientFormState> = _formState.asStateFlow()

    private val _availableUnits = MutableStateFlow<List<Units>>(emptyList())
    val availableUnits: StateFlow<List<Units>> = _availableUnits.asStateFlow()

    private val networkMonitor = NetworkMonitor(application)

    init {
        observeNetworkStatus()
        fetchUnits()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _uiState.update { it.copy(isNetworkAvailable = connected) }
            }
        }
    }

    private fun fetchUnits() {
        viewModelScope.launch {
            try {
                _availableUnits.value = repository.getUnits()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateFormName(name: String) = _formState.update { IngredientFormHelper.updateName(it, name) }
    fun updateFormCategory(category: IngredientCategory) = _formState.update { IngredientFormHelper.updateCategory(it, category) }
    fun updateFormDescription(desc: String) = _formState.update { IngredientFormHelper.updateDescription(it, desc) }
    fun addUnitRow() = _formState.update { IngredientFormHelper.addUnitRow(it) }
    fun updateUnitRow(index: Int, unit: Units?, calories: String) = _formState.update { IngredientFormHelper.updateUnitRow(it, index, unit, calories) }
    fun removeUnitRow(index: Int) = _formState.update { IngredientFormHelper.removeUnitRow(it, index) }

    fun validateForm(): Boolean {
        val (isValid, updatedState) = IngredientFormHelper.validateForm(_formState.value)
        _formState.value = updatedState
        return isValid
    }

    fun submitIngredient(imageUrl: String?, onComplete: () -> Unit) {
        if (!validateForm()) return

        val state = _formState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                // Check if name already exists in master catalog
                val existingCatalogName = repository.findExistingIngredientName(state.ingredientName)
                if (existingCatalogName != null) {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _formState.update {
                        it.copy(
                            nameError = R.string.ingredients_error_name_exists,
                            nameErrorArg = existingCatalogName
                        )
                    }
                    return@launch
                }

                // Check if name already exists in pending/approved ingredient requests
                val existingRequestName = requestRepository.findExistingRequestName(state.ingredientName)
                if (existingRequestName != null) {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _formState.update {
                        it.copy(
                            nameError = R.string.ingredients_error_request_exists,
                            nameErrorArg = existingRequestName
                        )
                    }
                    return@launch
                }
                val ingredientId = repository.getNextIngredientId()
                val filledRows = state.unitRows.filter { it.selectedUnit != null && it.calories.isNotBlank() }
                val unitIds = repository.getNextIngredientUnitIds(filledRows.size)

                val ingredient = Ingredients(
                    ingredientId = ingredientId,
                    ingredientName = state.ingredientName,
                    ingredientCategory = state.category,
                    ingredientDesc = state.description,
                    ingredientImage = imageUrl
                )

                val ingredientUnitsList = filledRows.mapIndexed { index, row ->
                    IngredientUnits(
                        ingredientUnitId = unitIds[index],
                        ingredientID = ingredientId,
                        unitID = row.selectedUnit!!.unitID,
                        caloriesPerDefaultQuantity = row.calories.toDoubleOrNull() ?: 0.0
                    )
                }

                repository.insertIngredient(ingredient)
                repository.insertIngredientUnits(ingredientUnitsList)

                _uiState.update { it.copy(isSubmitting = false) }
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        //TODO: errorMessage = getApplication<Application>().getString(R.string.admin_error_submission_failed, e.message ?: R.string.error_unknown)
                        errorMessage = R.string.error_unknown
                    )
                }
            }
        }
    }
}

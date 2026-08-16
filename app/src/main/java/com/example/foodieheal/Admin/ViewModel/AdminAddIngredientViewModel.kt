package com.example.foodieheal.Admin.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminAddIngredientUiState(
    val isNetworkAvailable: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

data class UnitRowState(
    val selectedUnit: Units? = null,
    val calories: String = "",
    val unitError: String? = null,
    val caloriesError: String? = null,
)

data class AdminAddIngredientFormState(
    val ingredientName: String = "",
    val category: IngredientCategory? = null,
    val description: String = "",
    val imageUrl: String? = null,
    val unitRows: List<UnitRowState> = listOf(UnitRowState()),
    
    // Per-field validation errors
    val nameError: String? = null,
    val categoryError: String? = null,
    val descriptionError: String? = null,
    val unitRowsError: String? = null
)

class AdminAddIngredientViewModel(
    application: Application,
    private val repository: IngredientsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AdminAddIngredientUiState())
    val uiState: StateFlow<AdminAddIngredientUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(AdminAddIngredientFormState())
    val formState: StateFlow<AdminAddIngredientFormState> = _formState.asStateFlow()

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

    fun updateFormName(name: String) = _formState.update { it.copy(ingredientName = name, nameError = null) }
    fun updateFormCategory(category: IngredientCategory) = _formState.update { it.copy(category = category, categoryError = null) }
    fun updateFormDescription(desc: String) = _formState.update { it.copy(description = desc, descriptionError = null) }
    
    fun addUnitRow() {
        _formState.update { it.copy(unitRows = it.unitRows + UnitRowState(), unitRowsError = null) }
    }

    fun updateUnitRow(index: Int, unit: Units?, calories: String) {
        _formState.update { state ->
            val newList = state.unitRows.toMutableList()
            newList[index] = newList[index].copy(selectedUnit = unit, calories = calories, unitError = null, caloriesError = null)
            state.copy(unitRows = newList, unitRowsError = null)
        }
    }

    fun removeUnitRow(index: Int) {
        _formState.update { state ->
            if (state.unitRows.size > 1) {
                val newList = state.unitRows.toMutableList()
                newList.removeAt(index)
                state.copy(unitRows = newList)
            } else state
        }
    }

    fun validateForm(): Boolean {
        val state = _formState.value
        var isValid = true

        val nameError = if (state.ingredientName.isBlank()) {
            isValid = false
            "Ingredient name is required"
        } else null

        val categoryError = if (state.category == null) {
            isValid = false
            "Category is required"
        } else null

        val descriptionError = if (state.description.isBlank()) {
            isValid = false
            "Description is required"
        } else null

        val hasAtLeastOneFilledRow = state.unitRows.any { it.selectedUnit != null && it.calories.isNotBlank() }
        val unitRowsError = if (!hasAtLeastOneFilledRow) {
            isValid = false
            "At least one calorie information entry is required"
        } else null

        val validatedRows = state.unitRows.map { row ->
            val unitError = if (row.selectedUnit == null && row.calories.isNotBlank()) {
                isValid = false
                "Serving unit is required"
            } else null

            val caloriesError = if (row.selectedUnit != null && row.calories.isBlank()) {
                isValid = false
                "Calories value is required"
            } else if (row.calories.isNotBlank() && row.calories.toDoubleOrNull() == null) {
                isValid = false
                "Must be a valid number"
            } else null

            row.copy(unitError = unitError, caloriesError = caloriesError)
        }

        _formState.update {
            it.copy(
                nameError = nameError,
                categoryError = categoryError,
                descriptionError = descriptionError,
                unitRowsError = unitRowsError,
                unitRows = validatedRows
            )
        }

        return isValid
    }

    fun submitIngredient(imageUrl: String?, onComplete: () -> Unit) {
        if (!validateForm()) return

        val state = _formState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
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
                _uiState.update { it.copy(isSubmitting = false, errorMessage = "Submission failed: ${e.message}") }
            }
        }
    }
}

package com.example.foodieheal.Admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.local.IngredientsDatabase
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.model.Status
import com.example.foodieheal.repo.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AdminIngredientActionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IngredientRequestRepository()
    private val userRepository = UserRepository()
    private val database = IngredientsDatabase.getInstance(application)
    private val productionRepository = IngredientsRepository(database.ingredientsDao())

    private val _requestDetail = MutableStateFlow<AdminIngredientRequestItem?>(null)
    val requestDetail: StateFlow<AdminIngredientRequestItem?> = _requestDetail.asStateFlow()

    private val _formState = MutableStateFlow(IngredientRequestFormUiState())
    val formState: StateFlow<IngredientRequestFormUiState> = _formState.asStateFlow()

    private val _availableUnits = MutableStateFlow<List<Units>>(emptyList())
    val availableUnits: StateFlow<List<Units>> = _availableUnits.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchUnits()
    }

    fun fetchRequestDetail(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = repository.getIngredientRequestById(requestId)
                if (request != null) {
                    val user = userRepository.getUserById(request.createdByUserId)
                    val allUnits = repository.getUnits().associateBy { it.unitID }
                    val unitRequests = repository.getIngredientUnitsRequestsById(requestId)
                    
                    val summary = unitRequests.joinToString("\n") { ur ->
                        val unit = allUnits[ur.unitID]
                        val qty = unit?.defaultQuantity?.toInt() ?: 100
                        val display = unit?.unitDisplay ?: ""
                        "${ur.caloriesPerDefaultQuantity.toInt()} kcal / ${qty} ${display}"
                    }
                    _requestDetail.value = AdminIngredientRequestItem(
                        request = request,
                        requesterName = user?.name ?: "Unknown",
                        requesterCustomId = user?.customId ?: "Unknown",
                        calorieSummary = summary
                    )
                }
                _isLoading.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    fun rejectRequest(requestId: String, reason: String, onComplete: () -> Unit) {
        if (reason.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateRequestStatus(requestId, Status.REJECTED, reason)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun populateFormForReview(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = repository.getIngredientRequestById(requestId)
                val unitRequests = repository.getIngredientUnitsRequestsById(requestId)
                val allUnits = repository.getUnits().associateBy { it.unitID }

                if (request != null) {
                    _formState.update { state ->
                        state.copy(
                            requestId = request.ingredientRequestId,
                            ingredientName = request.ingredientName,
                            category = request.ingredientCategory,
                            description = request.ingredientDesc,
                            imageUrl = request.ingredientImage,
                            unitRows = unitRequests.map { ur ->
                                UnitRowState(
                                    selectedUnit = allUnits[ur.unitID],
                                    calories = ur.caloriesPerDefaultQuantity.toInt().toString()
                                )
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveRequest(imageUrl: String?, onComplete: () -> Unit) {
        val state = _formState.value
        if (state.requestId == null) {
            _formState.update { it.copy(errorMessage = "Error: Request ID is missing") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                // 1. Generate IDs
                val ingredientId = productionRepository.getNextIngredientId()
                val unitIds = productionRepository.getNextIngredientUnitIds(state.unitRows.size)

                // 2. Create Production Ingredient
                val ingredient = Ingredients(
                    ingredientId = ingredientId,
                    ingredientName = state.ingredientName,
                    ingredientCategory = state.category,
                    ingredientDesc = state.description,
                    ingredientImage = imageUrl,
                    isDefault = true
                )
                productionRepository.insertIngredient(ingredient)

                // 3. Create Production Units
                val units = state.unitRows.mapIndexed { index, row ->
                    IngredientUnits(
                        ingredientUnitId = unitIds[index],
                        ingredientID = ingredientId,
                        unitID = row.selectedUnit?.unitID ?: "",
                        caloriesPerDefaultQuantity = row.calories.toDoubleOrNull() ?: 0.0
                    )
                }
                
                if (units.any { it.unitID.isEmpty() }) {
                    throw Exception("One or more serving units are not selected")
                }
                
                productionRepository.insertIngredientUnits(units)

                // 4. Update Request Status to APPROVED
                repository.updateRequestStatus(state.requestId, Status.APPROVED)

                _formState.update { it.copy(isSubmitting = false) }
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                _formState.update { it.copy(isSubmitting = false, errorMessage = "Approval failed: ${e.localizedMessage}") }
            }
        }
    }

    // Reuse form logic from the main VM if possible, but keeping it simple here
    fun updateFormName(name: String) = _formState.update { it.copy(ingredientName = name) }
    fun updateFormCategory(category: IngredientCategory) = _formState.update { it.copy(category = category) }
    fun updateFormDescription(desc: String) = _formState.update { it.copy(description = desc) }
    fun addUnitRow() = _formState.update { it.copy(unitRows = it.unitRows + UnitRowState()) }
    fun updateUnitRow(index: Int, unit: Units?, calories: String) = _formState.update { state ->
        val newList = state.unitRows.toMutableList()
        newList[index] = newList[index].copy(selectedUnit = unit, calories = calories)
        state.copy(unitRows = newList)
    }
    fun removeUnitRow(index: Int) = _formState.update { state ->
        if (state.unitRows.size > 1) {
            val newList = state.unitRows.toMutableList()
            newList.removeAt(index)
            state.copy(unitRows = newList)
        } else state
    }

    fun clearError() {
        _formState.update { it.copy(errorMessage = null) }
    }

    private fun fetchUnits() {
        viewModelScope.launch {
            try {
                _availableUnits.value = productionRepository.getUnits()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

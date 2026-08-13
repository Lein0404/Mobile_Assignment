package com.example.foodieheal.Admin.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.local.IngredientsDatabase
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.IngredientRequest
import com.example.foodieheal.ingredients.model.IngredientRequestFormUiState
import com.example.foodieheal.ingredients.model.IngredientUnits
import com.example.foodieheal.ingredients.model.IngredientUnitsRequest
import com.example.foodieheal.ingredients.model.Ingredients
import com.example.foodieheal.ingredients.model.UnitRowState
import com.example.foodieheal.ingredients.model.Units
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.model.Status
import com.example.foodieheal.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminIngredientRequestViewModel(application: Application) : AndroidViewModel(application) {

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

    fun rejectRequest(
        requestId: String,
        reason: String,
        onComplete: () -> Unit
    ) {
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
                    // Populate requestDetail for later use in approveRequest
                    val user = userRepository.getUserById(request.createdByUserId)
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

    fun approveRequest(
        imageUrl: String?,
        adminNote: String?,
        onComplete: () -> Unit
    ) {
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

                // 4. Synchronize and Update Request History (Original request records)
                val originalRequest = _requestDetail.value?.request
                if (originalRequest == null) {
                    throw Exception("Original request metadata missing. Please try again.")
                }

                val updatedRequestRecord = originalRequest.copy(
                    ingredientName = state.ingredientName,
                    ingredientCategory = state.category,
                    ingredientDesc = state.description,
                    ingredientImage = imageUrl,
                    requestStatus = Status.APPROVED,
                    rejectedReason = null, // Clear if it was previously rejected
                    adminNote = adminNote,
                    ingredientId = ingredientId
                )

                val unitRequestIds = repository.getNextUnitRequestIds(state.unitRows.size)
                val updatedUnitRequests = state.unitRows.mapIndexed { index, row ->
                    IngredientUnitsRequest(
                        ingredientUnitsRequestId = unitRequestIds[index],
                        ingredientRequestId = state.requestId,
                        unitID = row.selectedUnit?.unitID ?: "",
                        caloriesPerDefaultQuantity = row.calories.toDoubleOrNull() ?: 0.0
                    )
                }

                repository.updateIngredientRequest(updatedRequestRecord, updatedUnitRequests)

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

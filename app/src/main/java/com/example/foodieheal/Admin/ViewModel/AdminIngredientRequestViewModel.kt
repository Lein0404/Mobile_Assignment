package com.example.foodieheal.Admin.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.IngredientUnits
import com.example.foodieheal.ingredients.model.IngredientUnitsRequest
import com.example.foodieheal.ingredients.model.Ingredients
import com.example.foodieheal.ingredients.model.Units
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.viewModel.IngredientRequestFormUiState
import com.example.foodieheal.ingredients.viewModel.UnitRowState
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.model.Status
import com.example.foodieheal.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminIngredientRequestUiState(
    val isNetworkAvailable: Boolean = true,
    val isLoading: Boolean = false,
    val isDeletedByUser: Boolean = false,
    val isAlreadyProcessed: Boolean = false,
    val errorMessage: String? = null
)

class AdminIngredientRequestViewModel(
    application: Application,
    private val repository: IngredientRequestRepository,
    private val userRepository: UserRepository,
    private val productionRepository: IngredientsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AdminIngredientRequestUiState())
    val uiState: StateFlow<AdminIngredientRequestUiState> = _uiState.asStateFlow()

    private val _requestDetail = MutableStateFlow<AdminIngredientRequestItem?>(null)
    val requestDetail: StateFlow<AdminIngredientRequestItem?> = _requestDetail.asStateFlow()

    private val _formState = MutableStateFlow(IngredientRequestFormUiState())
    val formState: StateFlow<IngredientRequestFormUiState> = _formState.asStateFlow()

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

    fun fetchRequestDetail(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isDeletedByUser = false, isAlreadyProcessed = false) }
            try {
                val request = repository.getIngredientRequestById(requestId)
                if (request == null) {
                    _uiState.update { it.copy(isLoading = false, isDeletedByUser = true) }
                    return@launch
                }

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
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun populateFormForReview(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isDeletedByUser = false, isAlreadyProcessed = false) }
            try {
                val request = repository.getIngredientRequestById(requestId)
                // If the request is deleted by the user
                if (request == null) {
                    _uiState.update { it.copy(isLoading = false, isDeletedByUser = true) }
                    return@launch
                }
                // If the request is being approved by another admin at the meantime
                if (request.requestStatus != Status.PENDING) {
                    _uiState.update { it.copy(isLoading = false, isAlreadyProcessed = true) }
                    return@launch
                }
                // If the request still exists and pending, proceed

                val unitRequests = repository.getIngredientUnitsRequestsById(requestId)
                val allUnits = repository.getUnits().associateBy { it.unitID }

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
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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
            _uiState.update { it.copy(isLoading = true) }
            try {
                val current = repository.getIngredientRequestById(requestId)
                if (current == null) {
                    _uiState.update { it.copy(isLoading = false, isDeletedByUser = true) }
                    return@launch
                }
                if (current.requestStatus != Status.PENDING) {
                    _uiState.update { it.copy(isLoading = false, isAlreadyProcessed = true) }
                    return@launch
                }

                repository.updateRequestStatus(requestId, Status.REJECTED, reason)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun approveRequest(
        imageUrl: String?,
        adminNote: String?,
        onComplete: () -> Unit
    ) {
        if (!validateForm()) return

        val state = _formState.value
        if (state.requestId == null) {
            _formState.update { it.copy(errorMessage = "Error: Request ID is missing") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                // Check if request is still PENDING and exists
                val current = repository.getIngredientRequestById(state.requestId)
                if (current == null) {
                    _uiState.update { it.copy(isDeletedByUser = true) }
                    _formState.update { it.copy(isSubmitting = false) }
                    return@launch
                }
                if (current.requestStatus != Status.PENDING) {
                    _uiState.update { it.copy(isAlreadyProcessed = true) }
                    _formState.update { it.copy(isSubmitting = false) }
                    return@launch
                }

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
    fun updateFormName(name: String) = _formState.update { it.copy(ingredientName = name, nameError = null) }
    fun updateFormCategory(category: IngredientCategory) = _formState.update { it.copy(category = category, categoryError = null) }
    fun updateFormDescription(desc: String) = _formState.update { it.copy(description = desc, descriptionError = null) }
    fun addUnitRow() = _formState.update { it.copy(unitRows = it.unitRows + UnitRowState(), unitRowsError = null) }
    fun updateUnitRow(index: Int, unit: Units?, calories: String) = _formState.update { state ->
        val newList = state.unitRows.toMutableList()
        newList[index] = newList[index].copy(selectedUnit = unit, calories = calories, unitError = null, caloriesError = null)
        state.copy(unitRows = newList, unitRowsError = null)
    }
    fun removeUnitRow(index: Int) = _formState.update { state ->
        if (state.unitRows.size > 1) {
            val newList = state.unitRows.toMutableList()
            newList.removeAt(index)
            state.copy(unitRows = newList)
        } else state
    }

    /**
     * Validates all form fields and returns true if the form is valid.
     * Sets per-field error messages on the form state if invalid.
     */
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

        // Validate unit rows: at least one must be fully filled
        val hasAtLeastOneFilledRow = state.unitRows.any { it.selectedUnit != null && it.calories.isNotBlank() }
        val unitRowsError = if (!hasAtLeastOneFilledRow) {
            isValid = false
            "At least one calorie information entry is required"
        } else null

        // Per-row validation for partially filled rows
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

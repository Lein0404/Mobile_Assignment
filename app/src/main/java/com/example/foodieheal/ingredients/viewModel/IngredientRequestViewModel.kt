package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.model.Status
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class IngredientRequestUiState(
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val requests: List<IngredientRequestItem> = emptyList(),
    val filteredRequests: List<IngredientRequestItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isStatusConflict: Boolean = false,
    val errorMessage: String? = null,
    val isNetworkAvailable: Boolean = true
)

data class IngredientRequestFormUiState(
    val requestId: String? = null,
    val ingredientName: String = "",
    val category: IngredientCategory? = null,
    val description: String = "",
    val imageUrl: String? = null,
    val unitRows: List<UnitRowState> = listOf(UnitRowState()),
    val isSubmitting: Boolean = false,
    val isStatusConflict: Boolean = false,
    val errorMessage: String? = null,

    // Per-field validation errors
    val nameError: String? = null,
    val categoryError: String? = null,
    val descriptionError: String? = null,
    val unitRowsError: String? = null,
)

data class IngredientRequestItem(
    val request: IngredientRequest,
    val calorieSummary: String = ""
)

data class UnitRowState(
    val selectedUnit: Units? = null,
    val calories: String = "",
    val unitError: String? = null,
    val caloriesError: String? = null,
)

class IngredientRequestViewModel(
    application: Application,
    private val repository: IngredientRequestRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(IngredientRequestUiState())
    val uiState: StateFlow<IngredientRequestUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(IngredientRequestFormUiState())
    val formState: StateFlow<IngredientRequestFormUiState> = _formState.asStateFlow()

    private val _availableUnits = MutableStateFlow<List<Units>>(emptyList())
    val availableUnits: StateFlow<List<Units>> = _availableUnits.asStateFlow()

    private val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""

    private val _requestDetail = MutableStateFlow<IngredientRequestItem?>(null)
    val requestDetail: StateFlow<IngredientRequestItem?> = _requestDetail.asStateFlow()

    // Network connectivity monitoring
    private val networkMonitor = NetworkMonitor(application)

    init {
        observeNetworkStatus()
        fetchRequests()
        fetchUnits()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _uiState.update { it.copy(isNetworkAvailable = connected) }
                if (connected) {
                    // Refresh data when connectivity is restored
                    fetchRequests()
                }
            }
        }
    }

    fun fetchRequests(isRefreshing: Boolean = false) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            if (isRefreshing) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                val requests = repository.getIngredientRequests(currentUserId)
                val allUnits = repository.getUnits().associateBy { it.unitID }
                val unitRequests = repository.getIngredientUnitsRequests()

                val items = requests.map { request ->
                    val relevantUnits = unitRequests.filter { it.ingredientRequestId == request.ingredientRequestId }
                    val summary = relevantUnits.joinToString(", ") { ur ->
                        val unit = allUnits[ur.unitID]
                        val qty = unit?.defaultQuantity?.toInt() ?: 100
                        val display = unit?.unitDisplay ?: ""
                        "${ur.caloriesPerDefaultQuantity.toInt()}kcal/${qty}${display}"
                    }
                    IngredientRequestItem(request, summary)
                }
                _uiState.update { it.copy(requests = items, isLoading = false, isRefreshing = false) }
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = "Failed to fetch requests") }
            }
        }
    }

    fun refresh() {
        fetchRequests(isRefreshing = true)
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

    fun fetchRequestDetail(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val request = repository.getIngredientRequestById(requestId)
                if (request != null) {
                    val allUnits = repository.getUnits().associateBy { it.unitID }
                    val unitRequests = repository.getIngredientUnitsRequestsById(requestId)
                    
                    val summary = unitRequests.joinToString("\n") { ur ->
                        val unit = allUnits[ur.unitID]
                        val qty = unit?.defaultQuantity?.toInt() ?: 100
                        val display = unit?.unitDisplay ?: ""
                        "${ur.caloriesPerDefaultQuantity.toInt()} kcal / ${qty} ${display}"
                    }
                    _requestDetail.value = IngredientRequestItem(request, summary)
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to fetch request detail") }
            }
        }
    }

    fun deleteRequest(requestId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // Guard: Check status before deleting
                val current = repository.getIngredientRequestById(requestId)
                if (current != null && current.requestStatus != Status.PENDING) {
                    _uiState.update { it.copy(isStatusConflict = true) }
                    return@launch
                }

                repository.deleteIngredientRequest(requestId)
                fetchRequests()
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun populateFormForEdit(requestId: String) {
        viewModelScope.launch {
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
                            isStatusConflict = false,
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
            }
        }
    }

    fun clearForm() {
        _formState.value = IngredientRequestFormUiState()
    }

    fun clearStatusConflict() {
        _uiState.update { it.copy(isStatusConflict = false) }
        _formState.update { it.copy(isStatusConflict = false) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun toggleCategory(category: IngredientCategory) {
        _uiState.update { state ->
            val newCategories = if (state.selectedCategories.contains(category)) {
                state.selectedCategories - category
            } else {
                state.selectedCategories + category
            }
            state.copy(selectedCategories = newCategories)
        }
        applyFilters()
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val filtered = state.requests.filter { item ->
                (state.searchQuery.isEmpty() || item.request.ingredientName.contains(state.searchQuery, ignoreCase = true)) &&
                (state.selectedCategories.isEmpty() || item.request.ingredientCategory == null || state.selectedCategories.contains(item.request.ingredientCategory))
            }
            state.copy(filteredRequests = filtered)
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

    /**
     * Validates all form fields and returns true if the form is valid.
     * Sets per-field error messages on the form state if invalid.
     */
    private fun validateForm(): Boolean {
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

    fun submitRequest(
        imageUrl: String?,
        onComplete: () -> Unit
    ) {
        if (!validateForm()) return

        val state = _formState.value

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                // Check if request is still PENDING before updating
                if (state.requestId != null) {
                    val currentRequest = repository.getIngredientRequestById(state.requestId)
                    if (currentRequest?.requestStatus != Status.PENDING) {
                        _formState.update { it.copy(isSubmitting = false, isStatusConflict = true) }
                        return@launch
                    }
                }

                // Generate sequential IDs
                val requestId = state.requestId ?: repository.getNextRequestId()
                val filledRows = state.unitRows.filter { it.selectedUnit != null && it.calories.isNotBlank() }
                val unitIds = repository.getNextUnitRequestIds(filledRows.size)

                val request = IngredientRequest(
                    ingredientRequestId = requestId,
                    ingredientName = state.ingredientName,
                    ingredientCategory = state.category,
                    ingredientDesc = state.description,
                    ingredientImage = imageUrl,
                    createdByUserId = currentUserId,
                    requestStatus = Status.PENDING,
                    datetimeCreated = ZonedDateTime.now(ZoneId.of("Asia/Kuala_Lumpur"))
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )

                val unitRequests = filledRows.mapIndexed { index, row ->
                    IngredientUnitsRequest(
                        ingredientUnitsRequestId = unitIds[index],
                        ingredientRequestId = requestId,
                        unitID = row.selectedUnit!!.unitID,
                        caloriesPerDefaultQuantity = row.calories.toDoubleOrNull() ?: 0.0
                    )
                }

                // If editing, use update function
                if (state.requestId != null) {
                    repository.updateIngredientRequest(request, unitRequests)
                } else {
                    repository.submitIngredientRequest(request, unitRequests)
                }

                _formState.value = IngredientRequestFormUiState() // Reset
                fetchRequests() // Refresh
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                _formState.update { it.copy(isSubmitting = false, errorMessage = "Submission failed: ${e.message}") }
            }
        }
    }
}

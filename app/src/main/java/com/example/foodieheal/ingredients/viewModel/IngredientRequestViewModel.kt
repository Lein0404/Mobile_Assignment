package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.R
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.notification.IngredientRequestNotificationHelper
import com.example.foodieheal.ingredients.notification.IngredientRequestStatusMonitor
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.shared.IngredientFormHelper
import com.example.foodieheal.ingredients.shared.IngredientFormState
import com.example.foodieheal.ingredients.shared.IngredientRequestFilterHelper
import com.example.foodieheal.ingredients.shared.UnitRowState
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.model.Status
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class IngredientRequestUiState(
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val selectedStatus: Status? = null,
    val createdDateStart: LocalDate? = null,
    val createdDateEnd: LocalDate? = null,
    val processedDateStart: LocalDate? = null,
    val processedDateEnd: LocalDate? = null,
    val tempSelectedStatus: Status? = null,
    val tempCreatedDateStart: LocalDate? = null,
    val tempCreatedDateEnd: LocalDate? = null,
    val tempProcessedDateStart: LocalDate? = null,
    val tempProcessedDateEnd: LocalDate? = null,
    val showFilterSheet: Boolean = false,
    val requests: List<IngredientRequestItem> = emptyList(),
    val filteredRequests: List<IngredientRequestItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isStatusConflict: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val errorMessage: Int? = null,
    val showDeleteDialog: Boolean = false,
    val isCategoriesExpanded: Boolean = false
)

data class IngredientRequestItem(
    val request: IngredientRequest,
    val calorieSummary: String = ""
)

class IngredientRequestViewModel(
    application: Application,
    private val repository: IngredientRequestRepository,
    private val ingredientsRepository: IngredientsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(IngredientRequestUiState())
    val uiState: StateFlow<IngredientRequestUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(IngredientFormState())
    val formState: StateFlow<IngredientFormState> = _formState.asStateFlow()

    private val _availableUnits = MutableStateFlow<List<Units>>(emptyList())
    val availableUnits: StateFlow<List<Units>> = _availableUnits.asStateFlow()

    private val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""

    private val _requestDetail = MutableStateFlow<IngredientRequestItem?>(null)
    val requestDetail: StateFlow<IngredientRequestItem?> = _requestDetail.asStateFlow()

    // Network connectivity monitoring
    private val networkMonitor = NetworkMonitor(application)

    init {
        IngredientRequestNotificationHelper.createNotificationChannel(application)
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
                checkAndNotifyStatusUpdates(requests)

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
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = R.string.ingredients_error_fetch_requests) }
            }
        }
    }

    private fun checkAndNotifyStatusUpdates(requests: List<IngredientRequest>) {
        IngredientRequestStatusMonitor.processRequestList(requests, getApplication())
    }

    fun refresh() {
        fetchRequests(isRefreshing = true)
    }

    fun toggleCategoriesExpanded() {
        _uiState.update { it.copy(isCategoriesExpanded = !it.isCategoriesExpanded) }
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

    fun fetchRequestDetail(
        requestId: String,
        isRefreshing: Boolean = false
    ) {
        viewModelScope.launch {
            if (isRefreshing) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }
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
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = R.string.ingredients_error_fetch_details) }
            }
        }
    }

    fun refreshRequestDetail(requestId: String) {
        fetchRequestDetail(requestId, isRefreshing = true)
    }

    fun onShowDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }

    fun onShowFilterSheet(show: Boolean) {
        _uiState.update {
            it.copy(
                showFilterSheet = show,
                tempSelectedStatus = it.selectedStatus,
                tempCreatedDateStart = it.createdDateStart,
                tempCreatedDateEnd = it.createdDateEnd,
                tempProcessedDateStart = it.processedDateStart,
                tempProcessedDateEnd = it.processedDateEnd
            )
        }
    }

    fun updateTempStatus(status: Status?) {
        _uiState.update { it.copy(tempSelectedStatus = status) }
    }

    fun updateTempCreatedStartDate(date: LocalDate?) {
        _uiState.update { it.copy(tempCreatedDateStart = date) }
    }

    fun updateTempCreatedEndDate(date: LocalDate?) {
        _uiState.update { it.copy(tempCreatedDateEnd = date) }
    }

    fun updateTempProcessedStartDate(date: LocalDate?) {
        _uiState.update { it.copy(tempProcessedDateStart = date) }
    }

    fun updateTempProcessedEndDate(date: LocalDate?) {
        _uiState.update { it.copy(tempProcessedDateEnd = date) }
    }

    fun resetTempFilters() {
        _uiState.update {
            it.copy(
                tempSelectedStatus = null,
                tempCreatedDateStart = null,
                tempCreatedDateEnd = null,
                tempProcessedDateStart = null,
                tempProcessedDateEnd = null
            )
        }
    }

    fun applyFilterSheet() {
        _uiState.update {
            it.copy(
                showFilterSheet = false,
                selectedStatus = it.tempSelectedStatus,
                createdDateStart = it.tempCreatedDateStart,
                createdDateEnd = it.tempCreatedDateEnd,
                processedDateStart = it.tempProcessedDateStart,
                processedDateEnd = it.tempProcessedDateEnd
            )
        }
        applyFilters()
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
        _formState.value = IngredientFormState()
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
            val filtered = IngredientRequestFilterHelper.filterRequests(
                items = state.requests,
                searchQuery = state.searchQuery,
                selectedCategories = state.selectedCategories,
                selectedStatus = state.selectedStatus,
                createdDateStart = state.createdDateStart,
                createdDateEnd = state.createdDateEnd,
                processedDateStart = state.processedDateStart,
                processedDateEnd = state.processedDateEnd,
                getRequest = { it.request }
            )
            state.copy(filteredRequests = filtered)
        }
    }

    fun updateFormName(name: String) = _formState.update { IngredientFormHelper.updateName(it, name) }
    fun updateFormCategory(category: IngredientCategory) = _formState.update { IngredientFormHelper.updateCategory(it, category) }
    fun updateFormDescription(desc: String) = _formState.update { IngredientFormHelper.updateDescription(it, desc) }
    fun addUnitRow() = _formState.update { IngredientFormHelper.addUnitRow(it) }

    fun updateUnitRow(index: Int, unit: Units?, calories: String) {
        _formState.update { IngredientFormHelper.updateUnitRow(it, index, unit, calories) }
    }

    fun removeUnitRow(index: Int) {
        _formState.update { IngredientFormHelper.removeUnitRow(it, index) }
    }

    private fun validateForm(): Boolean {
        val (isValid, updatedState) = IngredientFormHelper.validateForm(_formState.value)
        _formState.value = updatedState
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
                // Check if name already exists in master catalog
                val existingCatalogName = ingredientsRepository.findExistingIngredientName(state.ingredientName)
                if (existingCatalogName != null) {
                    _formState.update {
                        it.copy(
                            isSubmitting = false,
                            nameError = R.string.ingredients_error_name_exists,
                            nameErrorArg = existingCatalogName
                        )
                    }
                    return@launch
                }

                // Check if name already exists in pending/approved ingredient requests
                val existingRequestName = repository.findExistingRequestName(state.ingredientName, excludeRequestId = state.requestId)
                if (existingRequestName != null) {
                    _formState.update {
                        it.copy(
                            isSubmitting = false,
                            nameError = R.string.ingredients_error_request_exists,
                            nameErrorArg = existingRequestName
                        )
                    }
                    return@launch
                }

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
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    datetimeProcessed = null
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

                _formState.value = IngredientFormState() // Reset
                fetchRequests() // Refresh
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = R.string.error_unknown
                    )
                }
            }
        }
    }
}

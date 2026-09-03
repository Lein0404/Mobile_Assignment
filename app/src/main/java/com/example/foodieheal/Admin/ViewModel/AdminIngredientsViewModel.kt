package com.example.foodieheal.Admin.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.IngredientRequest
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.ingredients.shared.IngredientRequestFilterHelper
import com.example.foodieheal.model.Status
import com.example.foodieheal.User.Repo.UserRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminIngredientsUiState(
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val selectedStatus: Status? = null, // null means "All"
    val createdDateStart: LocalDate? = null,
    val createdDateEnd: LocalDate? = null,
    val processedDateStart: LocalDate? = null,
    val processedDateEnd: LocalDate? = null,
    val tempSelectedCategories: Set<IngredientCategory> = emptySet(),
    val tempSelectedStatus: Status? = null,
    val tempCreatedDateStart: LocalDate? = null,
    val tempCreatedDateEnd: LocalDate? = null,
    val tempProcessedDateStart: LocalDate? = null,
    val tempProcessedDateEnd: LocalDate? = null,
    val showFilterSheet: Boolean = false,
    val requests: List<AdminIngredientRequestItem> = emptyList(),
    val filteredRequests: List<AdminIngredientRequestItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val errorMessage: Int? = null,
    val isCategoriesExpanded: Boolean = false
) {
    val totalCount: Int get() = requests.size
    val pendingCount: Int get() = requests.count { it.request.requestStatus == Status.PENDING }
    val approvedCount: Int get() = requests.count { it.request.requestStatus == Status.APPROVED }
    val rejectedCount: Int get() = requests.count { it.request.requestStatus == Status.REJECTED }
}

data class AdminIngredientRequestItem(
    val request: IngredientRequest,
    val requesterName: String = "Unknown",
    val requesterCustomId: String = "Unknown",
    val calorieSummary: String = ""
)

class AdminIngredientsViewModel(
    application: Application,
    private val repository: IngredientRequestRepository,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AdminIngredientsUiState())
    val uiState: StateFlow<AdminIngredientsUiState> = _uiState.asStateFlow()

    private val networkMonitor = NetworkMonitor(application)

    init {
        observeNetworkStatus()
        fetchRequests()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _uiState.update { it.copy(isNetworkAvailable = connected) }
                if (connected) {
                    fetchRequests()
                }
            }
        }
    }

    fun fetchRequests(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            if (isRefreshing) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                val requests = repository.getAllIngredientRequests()
                val users = userRepository.getAllUsers().associateBy { it.id }
                val allUnits = repository.getUnits().associateBy { it.unitID }
                val unitRequests = repository.getIngredientUnitsRequests()

                val items = requests.map { request ->
                    val user = users[request.createdByUserId]
                    val relevantUnits = unitRequests.filter { it.ingredientRequestId == request.ingredientRequestId }
                    val summary = relevantUnits.joinToString(", ") { ur ->
                        val unit = allUnits[ur.unitID]
                        val qty = unit?.defaultQuantity?.toInt() ?: 100
                        val display = unit?.unitDisplay ?: ""
                        "${ur.caloriesPerDefaultQuantity.toInt()}kcal/${qty}${display}"
                    }
                    AdminIngredientRequestItem(
                        request = request,
                        requesterName = user?.name ?: "Unknown",
                        requesterCustomId = user?.customId ?: "Unknown",
                        calorieSummary = summary
                    )
                }
                _uiState.update { it.copy(requests = items, isLoading = false, isRefreshing = false) }
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = R.string.admin_error_fetch_requests) }
            }
        }
    }

    fun refresh() {
        fetchRequests(isRefreshing = true)
    }

    fun onTabChange(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleCategoriesExpanded() {
        _uiState.update { it.copy(isCategoriesExpanded = !it.isCategoriesExpanded) }
    }

    fun onShowFilterSheet(show: Boolean) {
        _uiState.update {
            it.copy(
                showFilterSheet = show,
                tempSelectedCategories = it.selectedCategories,
                tempSelectedStatus = it.selectedStatus,
                tempCreatedDateStart = it.createdDateStart,
                tempCreatedDateEnd = it.createdDateEnd,
                tempProcessedDateStart = it.processedDateStart,
                tempProcessedDateEnd = it.processedDateEnd
            )
        }
    }

    fun onStatusTabSelected(status: Status?) {
        _uiState.update { it.copy(selectedStatus = status, tempSelectedStatus = status) }
        applyFilters()
    }

    fun updateTempCategory(category: IngredientCategory) {
        _uiState.update { state ->
            val newCategories = if (state.tempSelectedCategories.contains(category)) {
                state.tempSelectedCategories - category
            } else {
                state.tempSelectedCategories + category
            }
            state.copy(tempSelectedCategories = newCategories)
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
                tempSelectedCategories = emptySet(),
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
                selectedCategories = it.tempSelectedCategories,
                selectedStatus = it.tempSelectedStatus,
                createdDateStart = it.tempCreatedDateStart,
                createdDateEnd = it.tempCreatedDateEnd,
                processedDateStart = it.tempProcessedDateStart,
                processedDateEnd = it.tempProcessedDateEnd
            )
        }
        applyFilters()
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
}


package com.example.foodieheal.Admin.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.IngredientRequest
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.model.Status
import com.example.foodieheal.User.Repo.UserRepository
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
    val tempSelectedStatus: Status? = null,
    val requests: List<AdminIngredientRequestItem> = emptyList(),
    val filteredRequests: List<AdminIngredientRequestItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val errorMessage: Int? = null,
    val showStatusFilterDialog: Boolean = false,
)

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

    fun onShowStatusFilterDialog(show: Boolean) {
        _uiState.update { it.copy(showStatusFilterDialog = show, tempSelectedStatus = it.selectedStatus) }
    }

    fun updateTempStatus(status: Status?) {
        _uiState.update { it.copy(tempSelectedStatus = status) }
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

    fun onStatusFilterChange(status: Status?) {
        _uiState.update { it.copy(selectedStatus = status) }
        applyFilters()
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val filtered = state.requests.filter { item ->
                (state.searchQuery.isEmpty() || item.request.ingredientName.contains(state.searchQuery, ignoreCase = true)) &&
                (state.selectedCategories.isEmpty() || item.request.ingredientCategory == null || state.selectedCategories.contains(item.request.ingredientCategory)) &&
                (state.selectedStatus == null || item.request.requestStatus == state.selectedStatus)
            }
            state.copy(filteredRequests = filtered)
        }
    }
}


package com.example.foodieheal.Admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.model.Status
import com.example.foodieheal.repo.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminIngredientRequestUiState(
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val selectedStatus: Status? = null, // null means "All"
    val requests: List<AdminIngredientRequestItem> = emptyList(),
    val filteredRequests: List<AdminIngredientRequestItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class AdminIngredientRequestItem(
    val request: IngredientRequest,
    val requesterName: String = "Unknown",
    val requesterCustomId: String = "Unknown",
    val calorieSummary: String = ""
)

class AdminIngredientsViewModel(
    private val repository: IngredientRequestRepository = IngredientRequestRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminIngredientRequestUiState())
    val uiState: StateFlow<AdminIngredientRequestUiState> = _uiState.asStateFlow()

    init {
        fetchRequests()
    }

    fun fetchRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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
                _uiState.update { it.copy(requests = items, isLoading = false) }
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to fetch requests") }
            }
        }
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

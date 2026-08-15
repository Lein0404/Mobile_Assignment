package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.local.IngredientsDatabase
import com.example.foodieheal.ingredients.local.ShoppingListEntity
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.repo.ShoppingListRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class IngredientsUiState(
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val ingredients: List<IngredientItem> = emptyList(),
    val filteredIngredients: List<IngredientItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val ingredientDetail: IngredientDetailInfo? = null,
    val errorMessage: String? = null,
    val isNetworkAvailable: Boolean = true
)

data class IngredientItem(
    val ingredient: Ingredients,
    val calorieSummary: String = ""
)

data class IngredientDetailInfo(
    val ingredient: Ingredients,
    val calorieEntries: List<CalorieEntry> = emptyList(),
    val calorieSummary: String = ""
)

data class CalorieEntry(
    val calories: Double,
    val quantity: Double,
    val unitName: String
)

class IngredientsViewModel(
    application: Application,
    private val repository: IngredientsRepository,
    private val shoppingRepo: ShoppingListRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(IngredientsUiState())
    val uiState: StateFlow<IngredientsUiState> = _uiState.asStateFlow()

    // Network connectivity monitoring
    private val networkMonitor = NetworkMonitor(application)

    init {
        observeNetworkStatus()
        fetchIngredients()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _uiState.update { it.copy(isNetworkAvailable = connected) }
                if (connected) {
                    fetchIngredients()
                }
            }
        }
    }

    fun fetchIngredients(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            if (isRefreshing) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                val ingredients = repository.getIngredients()
                val allUnits = repository.getUnits().associateBy { it.unitID }
                val allIngredientUnits = repository.getAllIngredientUnits()

                val ingredientItems = ingredients.map { ingredient ->
                    val unitsForIngredient = allIngredientUnits.filter { it.ingredientID == ingredient.ingredientId }
                    val summary = unitsForIngredient.joinToString(", ") { iu ->
                        val unit = allUnits[iu.unitID]
                        val qty = unit?.defaultQuantity?.toInt() ?: 0
                        val display = unit?.unitDisplay ?: ""
                        "${iu.caloriesPerDefaultQuantity.toInt()}kcal/${qty}${display}"
                    }
                    IngredientItem(ingredient, summary)
                }

                _uiState.update { it.copy(ingredients = ingredientItems, errorMessage = null, isRefreshing = false) }
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = "Failed to fetch ingredients", isRefreshing = false) }
            } finally {
                updateLoading(false)
            }
        }
    }

    fun refresh() {
        fetchIngredients(isRefreshing = true)
    }

    fun onTabChange(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
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
            val filtered = state.ingredients.filter { item ->
                val ingredient = item.ingredient
                (state.searchQuery.isEmpty() || ingredient.ingredientName.contains(state.searchQuery, ignoreCase = true)) &&
                (state.selectedCategories.isEmpty() || ingredient.ingredientCategory == null || state.selectedCategories.contains(ingredient.ingredientCategory))
            }
            state.copy(filteredIngredients = filtered)
        }
    }

    private fun updateLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    fun fetchIngredientDetail(
        id: String,
        isRefreshing: Boolean = false
    ) {
        viewModelScope.launch {
            if (isRefreshing) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                updateLoading(true)
            }
            try {
                val ingredient = repository.getIngredientById(id)
                if (ingredient != null) {
                    val unitsMapping = repository.getUnits().associateBy { it.unitID }
                    val ingredientUnits = repository.getIngredientUnits(id)
                    
                    val calorieEntries = ingredientUnits.mapNotNull { iu ->
                        unitsMapping[iu.unitID]?.let { unit ->
                            CalorieEntry(
                                calories = iu.caloriesPerDefaultQuantity,
                                quantity = unit.defaultQuantity,
                                unitName = unit.unitDisplay
                            )
                        }
                    }
                    val calorieSummary = calorieEntries.joinToString("\n") { entry ->
                        "${entry.calories.toInt()} kcal / ${entry.quantity.toInt()} ${entry.unitName}"
                    }
                    _uiState.update { it.copy(ingredientDetail = IngredientDetailInfo(ingredient, calorieEntries, calorieSummary), isRefreshing = false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = "Failed to fetch ingredient details", isRefreshing = false) }
            } finally {
                updateLoading(false)
            }
        }
    }

    fun refreshIngredientDetail(id: String) {
        fetchIngredientDetail(id, isRefreshing = true)
    }

    fun addToShoppingList(ingredient: Ingredients) {
        addToShoppingList(ingredient.ingredientId, ingredient.ingredientName)
    }

    fun addToShoppingList(ingredientId: String, ingredientName: String) {
        viewModelScope.launch {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
            if (userId.isEmpty()) return@launch

            val entity = ShoppingListEntity(
                userId = userId,
                ingredientId = ingredientId,
                ingredientName = ingredientName,
                isChecked = false
            )
            shoppingRepo.insertItem(entity)
        }
    }
}

package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.local.ShoppingListEntity
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.repo.ShoppingListRepository
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.R
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ShoppingListUiState(
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val items: List<ShoppingListItem> = emptyList(),
    val filteredItems: List<ShoppingListItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: Int? = null,
    val showClearCheckedDialog: Boolean = false,
    val showClearAllDialog: Boolean = false
)

class ShoppingListViewModel(
    application: Application,
    private val shoppingRepo: ShoppingListRepository,
    private val ingredientsRepo: IngredientsRepository
) : AndroidViewModel(application) {
    private val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val ingredients = try { ingredientsRepo.getIngredients() } catch (_: Exception) { emptyList() }
            val ingredientsMap = ingredients.associateBy { it.ingredientId }

            shoppingRepo.getShoppingList(currentUserId).collectLatest { entities ->
                val items = entities.map { entity ->
                    val ingredient = ingredientsMap[entity.ingredientId]
                    val category = ingredient?.ingredientCategory
                    val description = ingredient?.ingredientDesc ?: ""
                    
                    ShoppingListItem(entity, category, description)
                }
                
                _uiState.update { 
                    it.copy(
                        items = items,
                        isLoading = false,
                        errorMessage = if (ingredients.isEmpty()) R.string.shopping_list_error_local_data else null
                    ) 
                }
                applyFilters()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onShowClearCheckedDialog(show: Boolean) {
        _uiState.update { it.copy(showClearCheckedDialog = show) }
    }

    fun onShowClearAllDialog(show: Boolean) {
        _uiState.update { it.copy(showClearAllDialog = show) }
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
            val filtered = state.items.filter { item ->
                (state.searchQuery.isEmpty() || item.entity.ingredientName.contains(state.searchQuery, ignoreCase = true)) &&
                (state.selectedCategories.isEmpty() || item.category == null || state.selectedCategories.contains(item.category))
            }
            state.copy(filteredItems = filtered)
        }
    }

    fun toggleChecked(item: ShoppingListItem) {
        viewModelScope.launch {
            shoppingRepo.updateChecked(item.entity.id, !item.entity.isChecked)
        }
    }

    fun addItems(items: List<ShoppingListEntity>) {
        viewModelScope.launch {
            items.forEach { shoppingRepo.insertItem(it) }
        }
    }

    fun clearChecked() {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.clearChecked(currentUserId)
        }
    }

    fun clearAll() {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.clearAll(currentUserId)
        }
    }
}

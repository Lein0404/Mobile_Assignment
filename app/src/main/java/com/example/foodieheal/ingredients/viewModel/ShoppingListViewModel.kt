package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.local.ShoppingListItemEntity
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.repo.ShoppingListRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ShoppingListUiState(
    val shoppingLists: List<ShoppingList> = emptyList(),
    val filteredShoppingLists: List<ShoppingList> = emptyList(),
    val listSearchQuery: String = "",
    val selectedShoppingListId: String? = null,
    val activeShoppingList: ShoppingList? = null,
    val itemSearchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val isCategoriesExpanded: Boolean = false,
    val items: List<ShoppingListItem> = emptyList(),
    val filteredItems: List<ShoppingListItem> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateListDialog: Boolean = false,
    val showDeleteListDialog: Boolean = false,
    val listToDeleteId: String? = null,
    val showClearCheckedDialog: Boolean = false,
    val showClearAllDialog: Boolean = false,
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

            shoppingRepo.getShoppingLists(currentUserId).collectLatest { lists ->
                val currentSelectedId = _uiState.value.selectedShoppingListId
                val activeList = lists.find { it.shoppingListId == currentSelectedId }
                    ?: lists.firstOrNull()
                val items = activeList?.items ?: emptyList()

                _uiState.update { state ->
                    state.copy(
                        shoppingLists = lists,
                        selectedShoppingListId = activeList?.shoppingListId,
                        activeShoppingList = activeList,
                        items = items,
                        isLoading = false
                    )
                }
                applyListFilters()
                applyItemFilters()
            }
        }
    }

    // ──────────────── Shopping Lists Management ────────────────

    fun onListSearchQueryChange(query: String) {
        _uiState.update { it.copy(listSearchQuery = query) }
        applyListFilters()
    }

    private fun applyListFilters() {
        _uiState.update { state ->
            val query = state.listSearchQuery.trim()
            val filtered = if (query.isEmpty()) {
                state.shoppingLists
            } else {
                state.shoppingLists.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.shoppingListId.contains(query, ignoreCase = true)
                }
            }
            state.copy(filteredShoppingLists = filtered)
        }
    }

    fun selectShoppingList(shoppingListId: String) {
        val activeList = _uiState.value.shoppingLists.find { it.shoppingListId == shoppingListId }
        val items = activeList?.items ?: emptyList()

        _uiState.update {
            it.copy(
                selectedShoppingListId = shoppingListId,
                activeShoppingList = activeList,
                items = items,
                itemSearchQuery = "",
                selectedCategories = emptySet()
            )
        }
        applyItemFilters()
    }

    fun createNewShoppingList(name: String = "", onCreated: ((String) -> Unit)? = null) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            val title = name.trim().ifEmpty { null }
            val created = shoppingRepo.createShoppingList(currentUserId, title)
            _uiState.update { it.copy(selectedShoppingListId = created.shoppingListId, showCreateListDialog = false) }
            onCreated?.invoke(created.shoppingListId)
        }
    }

    fun deleteShoppingList(shoppingListId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.deleteShoppingList(shoppingListId, currentUserId)
            _uiState.update { it.copy(showDeleteListDialog = false, listToDeleteId = null) }
        }
    }

    fun updateShoppingListTitle(shoppingListId: String, newTitle: String) {
        if (currentUserId.isEmpty()) return
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.updateShoppingListTitle(shoppingListId, currentUserId, trimmed)
        }
    }

    fun onShowCreateListDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateListDialog = show) }
    }

    fun onShowDeleteListDialog(show: Boolean, listId: String? = null) {
        _uiState.update {
            it.copy(
                showDeleteListDialog = show,
                listToDeleteId = listId ?: it.selectedShoppingListId
            )
        }
    }

    // ──────────────── Active List Items Management ────────────────

    fun onItemSearchQueryChange(query: String) {
        _uiState.update { it.copy(itemSearchQuery = query) }
        applyItemFilters()
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
        applyItemFilters()
    }

    fun toggleCategoriesExpanded() {
        _uiState.update { it.copy(isCategoriesExpanded = !it.isCategoriesExpanded) }
    }

    private fun applyItemFilters() {
        _uiState.update { state ->
            val query = state.itemSearchQuery.trim()
            val filtered = state.items.filter { item ->
                (query.isEmpty() || item.ingredientName.contains(query, ignoreCase = true)) &&
                (state.selectedCategories.isEmpty() || item.category == null || state.selectedCategories.contains(item.category))
            }
            state.copy(filteredItems = filtered)
        }
    }

    fun toggleChecked(item: ShoppingListItem) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.updateChecked(
                id = item.id,
                shoppingListId = item.shoppingListId,
                userId = currentUserId,
                isChecked = !item.isChecked
            )
        }
    }

    fun addItems(shoppingListId: String, items: List<ShoppingListItemEntity>) {
        viewModelScope.launch {
            shoppingRepo.insertItems(items)
        }
    }

    fun onShowClearCheckedDialog(show: Boolean) {
        _uiState.update { it.copy(showClearCheckedDialog = show) }
    }

    fun onShowClearAllDialog(show: Boolean) {
        _uiState.update { it.copy(showClearAllDialog = show) }
    }

    fun clearChecked(shoppingListId: String? = null) {
        val targetId = shoppingListId ?: _uiState.value.selectedShoppingListId ?: return
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.clearChecked(targetId, currentUserId)
            _uiState.update { it.copy(showClearCheckedDialog = false) }
        }
    }

    fun clearAll(shoppingListId: String? = null) {
        val targetId = shoppingListId ?: _uiState.value.selectedShoppingListId ?: return
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.clearAll(targetId, currentUserId)
            _uiState.update { it.copy(showClearAllDialog = false) }
        }
    }
}

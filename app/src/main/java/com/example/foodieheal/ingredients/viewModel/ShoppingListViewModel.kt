package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.local.IngredientsEntity
import com.example.foodieheal.ingredients.local.ShoppingListItemEntity
import com.example.foodieheal.ingredients.local.toEntity
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.repo.ShoppingListRepository
import com.example.foodieheal.ingredients.shared.ShoppingListShareHelper
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * `ShoppingListUiState` is split (modularized) from a single "God State"
 * into 4 sub-data classes which handles the states for each of the 4 Shopping List screens
 */
data class ShoppingListUiState(
    val isLoading: Boolean = false,
    val homeState: ShoppingListHomeUiState = ShoppingListHomeUiState(),
    val detailState: ShoppingListDetailUiState = ShoppingListDetailUiState(),
    val addItemState: ShoppingListAddItemUiState = ShoppingListAddItemUiState(),
    val addFromState: ShoppingListAddFromUiState = ShoppingListAddFromUiState()
)

data class ShoppingListHomeUiState(
    val shoppingLists: List<ShoppingList> = emptyList(),
    val filteredShoppingLists: List<ShoppingList> = emptyList(),
    val searchQuery: String = "",
    val showCreateDialog: Boolean = false,
    val newListNameInput: String = "",
    val showDeleteDialog: Boolean = false,
    val listToDeleteId: String? = null,
    val showChangeDefaultDialog: Boolean = false,
    val targetListForDefault: ShoppingList? = null
)

data class ShoppingListDetailUiState(
    val selectedShoppingListId: String? = null,
    val activeShoppingList: ShoppingList? = null,
    val items: List<ShoppingListItem> = emptyList(),
    val filteredItems: List<ShoppingListItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategories: Set<IngredientCategory> = emptySet(),
    val isCategoriesExpanded: Boolean = false,
    val editableTitle: String = "",
    val showUnsavedChangesDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showClearCheckedDialog: Boolean = false,
    val showClearAllDialog: Boolean = false,
    val showChangeDefaultDialog: Boolean = false
)

data class ShoppingListAddItemUiState(
    val selectedIngredients: List<IngredientItem> = emptyList()
)

data class ShoppingListAddFromUiState(
    val parsedIngredients: List<IngredientsEntity> = emptyList(),
    val selectedIngredientIds: Set<String> = emptySet(),
    val isParsed: Boolean = false,
    val selectedListId: String = "",
    val isNewList: Boolean = false,
    val newListNameInput: String = ""
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
                val currentSelectedId = _uiState.value.detailState.selectedShoppingListId
                val activeList = lists.find { it.shoppingListId == currentSelectedId }
                    ?: lists.firstOrNull()
                val items = activeList?.items ?: emptyList()

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        homeState = state.homeState.copy(
                            shoppingLists = lists
                        ),
                        detailState = state.detailState.copy(
                            selectedShoppingListId = activeList?.shoppingListId,
                            activeShoppingList = activeList,
                            items = items,
                            editableTitle = activeList?.title?.ifEmpty { activeList.shoppingListId } ?: ""
                        )
                    )
                }
                applyListFilters()
                applyItemFilters()
            }
        }
    }

    // ──────────────── Shopping Lists Management (Home Screen) ────────────────

    fun onListSearchQueryChange(query: String) {
        _uiState.update { it.copy(homeState = it.homeState.copy(searchQuery = query)) }
        applyListFilters()
    }

    private fun applyListFilters() {
        _uiState.update { state ->
            val query = state.homeState.searchQuery.trim()
            val filtered = if (query.isEmpty()) {
                state.homeState.shoppingLists
            } else {
                state.homeState.shoppingLists.filter { list ->
                    list.title.contains(query, ignoreCase = true) ||
                    list.shoppingListId.contains(query, ignoreCase = true) ||
                    list.items.any { item ->
                        item.ingredientName.contains(query, ignoreCase = true) ||
                        (item.category?.categoryName?.contains(query, ignoreCase = true) == true)
                    }
                }
            }
            state.copy(homeState = state.homeState.copy(filteredShoppingLists = filtered))
        }
    }

    fun selectShoppingList(shoppingListId: String) {
        val activeList = _uiState.value.homeState.shoppingLists.find { it.shoppingListId == shoppingListId }
        val items = activeList?.items ?: emptyList()

        _uiState.update {
            it.copy(
                detailState = it.detailState.copy(
                    selectedShoppingListId = shoppingListId,
                    activeShoppingList = activeList,
                    items = items,
                    searchQuery = "",
                    selectedCategories = emptySet(),
                    editableTitle = activeList?.title?.ifEmpty { activeList.shoppingListId } ?: ""
                )
            )
        }
        applyItemFilters()
    }

    fun createNewShoppingList(name: String = "", onCreated: ((String) -> Unit)? = null) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            val title = name.trim().ifEmpty { null }
            val created = shoppingRepo.createShoppingList(currentUserId, title)
            _uiState.update { 
                it.copy(
                    detailState = it.detailState.copy(selectedShoppingListId = created.shoppingListId),
                    homeState = it.homeState.copy(showCreateDialog = false, newListNameInput = "")
                ) 
            }
            onCreated?.invoke(created.shoppingListId)
        }
    }

    fun deleteShoppingList(shoppingListId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.deleteShoppingList(shoppingListId, currentUserId)
            _uiState.update { 
                it.copy(
                    homeState = it.homeState.copy(showDeleteDialog = false, listToDeleteId = null),
                    detailState = it.detailState.copy(showDeleteDialog = false)
                ) 
            }
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

    fun setDefaultShoppingList(shoppingListId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.setDefaultShoppingList(shoppingListId, currentUserId)
        }
    }

    fun deselectDefaultShoppingList(shoppingListId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.deselectDefaultShoppingList(shoppingListId, currentUserId)
        }
    }

    fun onShowCreateListDialog(show: Boolean) {
        _uiState.update { it.copy(homeState = it.homeState.copy(showCreateDialog = show)) }
    }

    fun updateNewListName(name: String) {
        _uiState.update { it.copy(homeState = it.homeState.copy(newListNameInput = name)) }
    }

    fun onShowHomeDeleteListDialog(show: Boolean, listId: String? = null) {
        _uiState.update {
            it.copy(
                homeState = it.homeState.copy(
                    showDeleteDialog = show,
                    listToDeleteId = listId ?: it.detailState.selectedShoppingListId
                )
            )
        }
    }

    fun onShowHomeChangeDefaultDialog(show: Boolean, targetList: ShoppingList? = null) {
        _uiState.update {
            it.copy(
                homeState = it.homeState.copy(
                    showChangeDefaultDialog = show,
                    targetListForDefault = targetList
                )
            )
        }
    }

    fun addIngredientsToShoppingList(
        shoppingListId: String,
        ingredients: List<IngredientsEntity>,
        onSuccess: ((Int) -> Unit)? = null
    ) {
        if (currentUserId.isEmpty() || ingredients.isEmpty()) return
        viewModelScope.launch {
            val items = ingredients.map { ing ->
                ShoppingListItemEntity(
                    shoppingListId = shoppingListId,
                    userId = currentUserId,
                    ingredientId = ing.ingredientId,
                    ingredientName = ing.ingredientName,
                    ingredientCategory = ing.ingredientCategory,
                    isChecked = false
                )
            }
            shoppingRepo.insertItems(items)
            onSuccess?.invoke(items.size)
        }
    }

    // ──────────────── Active List Items Management (Detail Screen) ────────────────

    fun onItemSearchQueryChange(query: String) {
        _uiState.update { it.copy(detailState = it.detailState.copy(searchQuery = query)) }
        applyItemFilters()
    }

    fun toggleCategory(category: IngredientCategory) {
        _uiState.update { state ->
            val newCategories = if (state.detailState.selectedCategories.contains(category)) {
                state.detailState.selectedCategories - category
            } else {
                state.detailState.selectedCategories + category
            }
            state.copy(detailState = state.detailState.copy(selectedCategories = newCategories))
        }
        applyItemFilters()
    }

    fun toggleCategoriesExpanded() {
        _uiState.update { it.copy(detailState = it.detailState.copy(isCategoriesExpanded = !it.detailState.isCategoriesExpanded)) }
    }

    private fun applyItemFilters() {
        _uiState.update { state ->
            val query = state.detailState.searchQuery.trim()
            val filtered = state.detailState.items.filter { item ->
                (query.isEmpty() || item.ingredientName.contains(query, ignoreCase = true)) &&
                (state.detailState.selectedCategories.isEmpty() || item.category == null || state.detailState.selectedCategories.contains(item.category))
            }
            state.copy(detailState = state.detailState.copy(filteredItems = filtered))
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
        _uiState.update { it.copy(detailState = it.detailState.copy(showClearCheckedDialog = show)) }
    }

    fun onShowClearAllDialog(show: Boolean) {
        _uiState.update { it.copy(detailState = it.detailState.copy(showClearAllDialog = show)) }
    }

    fun onShowDetailDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(detailState = it.detailState.copy(showDeleteDialog = show)) }
    }

    fun onShowDetailChangeDefaultDialog(show: Boolean) {
        _uiState.update { it.copy(detailState = it.detailState.copy(showChangeDefaultDialog = show)) }
    }

    fun onShowUnsavedChangesDialog(show: Boolean) {
        _uiState.update { it.copy(detailState = it.detailState.copy(showUnsavedChangesDialog = show)) }
    }

    fun updateEditableTitle(title: String) {
        _uiState.update { it.copy(detailState = it.detailState.copy(editableTitle = title)) }
    }

    fun clearChecked(shoppingListId: String? = null) {
        val targetId = shoppingListId ?: _uiState.value.detailState.selectedShoppingListId ?: return
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.clearChecked(targetId, currentUserId)
            _uiState.update { it.copy(detailState = it.detailState.copy(showClearCheckedDialog = false)) }
        }
    }

    fun clearAll(shoppingListId: String? = null) {
        val targetId = shoppingListId ?: _uiState.value.detailState.selectedShoppingListId ?: return
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            shoppingRepo.clearAll(targetId, currentUserId)
            _uiState.update { it.copy(detailState = it.detailState.copy(showClearAllDialog = false)) }
        }
    }

    // ──────────────── Add Item Screen ────────────────

    fun toggleIngredientSelection(item: IngredientItem) {
        _uiState.update { state ->
            val currentSelected = state.addItemState.selectedIngredients
            val isSelected = currentSelected.any { it.ingredient.ingredientId == item.ingredient.ingredientId }
            val newList = if (isSelected) {
                currentSelected.filter { it.ingredient.ingredientId != item.ingredient.ingredientId }
            } else {
                currentSelected + item
            }
            state.copy(addItemState = state.addItemState.copy(selectedIngredients = newList))
        }
    }

    fun clearAddItemSelection() {
        _uiState.update { it.copy(addItemState = it.addItemState.copy(selectedIngredients = emptyList())) }
    }

    // ──────────────── Add From Clipboard / Recipe Screen ────────────────

    fun updateAddFromSelectedListId(listId: String) {
        _uiState.update { it.copy(addFromState = it.addFromState.copy(selectedListId = listId)) }
    }

    fun updateAddFromIsNewList(isNew: Boolean) {
        _uiState.update { it.copy(addFromState = it.addFromState.copy(isNewList = isNew)) }
    }

    fun updateAddFromNewListName(name: String) {
        _uiState.update { it.copy(addFromState = it.addFromState.copy(newListNameInput = name)) }
    }

    fun createShoppingListAndAddIngredients(
        title: String,
        ingredients: List<IngredientsEntity>,
        onSuccess: (String, Int) -> Unit
    ) {
        if (currentUserId.isEmpty() || ingredients.isEmpty()) return
        viewModelScope.launch {
            val createdList = shoppingRepo.createShoppingList(currentUserId, title.trim().ifEmpty { null })
            val items = ingredients.map { ing ->
                ShoppingListItemEntity(
                    shoppingListId = createdList.shoppingListId,
                    userId = currentUserId,
                    ingredientId = ing.ingredientId,
                    ingredientName = ing.ingredientName,
                    ingredientCategory = ing.ingredientCategory,
                    isChecked = false
                )
            }
            shoppingRepo.insertItems(items)
            onSuccess(createdList.title.ifEmpty { createdList.shoppingListId }, items.size)
        }
    }

    fun refreshFromClipboard(context: android.content.Context, allIngredients: List<IngredientsEntity> = emptyList()) {
        viewModelScope.launch {
            val databaseIngredients = if (allIngredients.isNotEmpty()) {
                allIngredients
            } else {
                ingredientsRepo.getIngredients().map { it.toEntity() }
            }
            val rawText = ShoppingListShareHelper.getClipboardText(context)
            val validMatched = ShoppingListShareHelper.parseAndValidateClipboardText(rawText, databaseIngredients)
            _uiState.update {
                it.copy(
                    addFromState = it.addFromState.copy(
                        parsedIngredients = validMatched,
                        selectedIngredientIds = validMatched.map { ing -> ing.ingredientId }.toSet(),
                        isParsed = true
                    )
                )
            }
        }
    }

    fun setIngredientsFromRecipe(
        recipeIngredients: List<com.example.foodieheal.Recipe.Model.IngredientItem>,
        allIngredients: List<IngredientsEntity> = emptyList()
    ) {
        viewModelScope.launch {
            val databaseIngredients = if (allIngredients.isNotEmpty()) {
                allIngredients
            } else {
                ingredientsRepo.getIngredients().map { it.toEntity() }
            }

            val parsedList = recipeIngredients.mapIndexed { index, recipeIng ->
                val name = recipeIng.name.trim()
                val qty = recipeIng.displayQuantity.trim()
                val unit = recipeIng.unit.trim()

                // Option C format: e.g. "Frozen Chicken Nuggets (6 count)"
                val formattedName = when {
                    qty.isNotEmpty() && qty != "0" && unit.isNotEmpty() -> "$name ($qty $unit)"
                    qty.isNotEmpty() && qty != "0" -> "$name ($qty)"
                    unit.isNotEmpty() -> "$name ($unit)"
                    else -> name
                }

                // Match against cached ingredients to find official ID and Category
                val matched = databaseIngredients.find { it.ingredientName.equals(name, ignoreCase = true) }
                    ?: databaseIngredients.find { name.contains(it.ingredientName, ignoreCase = true) || it.ingredientName.contains(name, ignoreCase = true) }

                val resolvedCategory = matched?.ingredientCategory ?: IngredientCategory.OTHERS.name

                IngredientsEntity(
                    ingredientId = matched?.ingredientId ?: "RECIPE_ING_${index}_${name.hashCode()}",
                    ingredientName = formattedName,
                    ingredientCategory = resolvedCategory,
                    ingredientDesc = "",
                    ingredientImage = matched?.ingredientImage
                )
            }

            _uiState.update {
                it.copy(
                    addFromState = it.addFromState.copy(
                        parsedIngredients = parsedList,
                        selectedIngredientIds = parsedList.map { ing -> ing.ingredientId }.toSet(),
                        isParsed = true
                    )
                )
            }
        }
    }

    fun resetAddFromState() {
        _uiState.update {
            it.copy(addFromState = ShoppingListAddFromUiState())
        }
    }

    fun toggleAddFromIngredientSelection(ingredientId: String) {
        _uiState.update { state ->
            val currentSelected = state.addFromState.selectedIngredientIds
            val isSelected = currentSelected.contains(ingredientId)
            val newList = if (isSelected) {
                currentSelected - ingredientId
            } else {
                currentSelected + ingredientId
            }
            state.copy(addFromState = state.addFromState.copy(selectedIngredientIds = newList))
        }
    }

    fun setAddFromAllSelected(selectAll: Boolean) {
        _uiState.update { state ->
            val newList = if (selectAll) {
                state.addFromState.parsedIngredients.map { it.ingredientId }.toSet()
            } else {
                emptySet()
            }
            state.copy(addFromState = state.addFromState.copy(selectedIngredientIds = newList))
        }
    }
}

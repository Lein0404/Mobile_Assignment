package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.R
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.local.ShoppingListEntity
import com.example.foodieheal.ingredients.local.ShoppingListItemEntity
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
    val errorMessage: Int? = null,
    val isNetworkAvailable: Boolean = true,
    val isCategoriesExpanded: Boolean = false,
    val addShoppingListState: AddIngredientToShoppingListUiState = AddIngredientToShoppingListUiState()
)

/**
 * `AddIngredientToShoppingListUiState` is split (modularized) from `IngredientsUiState`
 * to enhance the readability and maintainability of the code
 */
data class AddIngredientToShoppingListUiState(
    val showDialog: Boolean = false,
    val isNewListOptionSelected: Boolean = false,
    val newListNameInput: String = "",
    val selectedListIndex: Int = 0,
    val pendingIngredient: AddToShoppingListTarget? = null,
    val availableLists: List<ShoppingListEntity> = emptyList()
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

data class AddToShoppingListTarget(
    val ingredientId: String,
    val ingredientName: String,
    val category: IngredientCategory? = null
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
                _uiState.update { it.copy(errorMessage = R.string.ingredients_error_fetch_ingredients, isRefreshing = false) }
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

    fun toggleCategoriesExpanded() {
        _uiState.update { it.copy(isCategoriesExpanded = !it.isCategoriesExpanded) }
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
            val query = state.searchQuery.trim()
            val filtered = state.ingredients.filter { item ->
                val ingredient = item.ingredient
                (query.isEmpty() ||
                 ingredient.ingredientName.contains(query, ignoreCase = true)) &&
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
                _uiState.update { it.copy(errorMessage = R.string.ingredients_error_fetch_details, isRefreshing = false) }
            } finally {
                updateLoading(false)
            }
        }
    }

    fun refreshIngredientDetail(id: String) {
        fetchIngredientDetail(id, isRefreshing = true)
    }

    fun addToShoppingList(ingredient: Ingredients) {
        addToShoppingList(ingredient.ingredientId, ingredient.ingredientName, ingredient.ingredientCategory)
    }

    fun addToShoppingList(ingredientId: String, ingredientName: String, category: IngredientCategory? = null) {
        viewModelScope.launch {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
            if (userId.isEmpty()) return@launch

            shoppingRepo.addItemToActiveOrCreateShoppingList(
                userId = userId,
                ingredientId = ingredientId,
                ingredientName = ingredientName,
                category = category
            )
        }
    }

    fun requestAddToShoppingList(
        ingredient: Ingredients,
        onAddedDirectly: ((listTitle: String) -> Unit)? = null
    ) {
        requestAddToShoppingList(ingredient.ingredientId, ingredient.ingredientName, ingredient.ingredientCategory, onAddedDirectly)
    }

    fun requestAddToShoppingList(
        ingredientId: String,
        ingredientName: String,
        category: IngredientCategory? = null,
        onAddedDirectly: ((listTitle: String) -> Unit)? = null
    ) {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
        if (userId.isEmpty()) return

        viewModelScope.launch {
            val defaultList = shoppingRepo.getDefaultShoppingList(userId)
            if (defaultList != null) {
                // Add directly to default list
                val item = ShoppingListItemEntity(
                    shoppingListId = defaultList.shoppingListId,
                    userId = userId,
                    ingredientId = ingredientId,
                    ingredientName = ingredientName,
                    ingredientCategory = category?.name,
                    isChecked = false
                )
                shoppingRepo.insertItem(item)
                val listTitle = defaultList.title.ifEmpty { defaultList.shoppingListId }
                onAddedDirectly?.invoke(listTitle)
                return@launch
            }

            // No default list: fetch existing lists
            val allLists = shoppingRepo.getShoppingListsForUser(userId)
            if (allLists.isEmpty()) {
                val newList = shoppingRepo.createShoppingList(userId)
                val item = ShoppingListItemEntity(
                    shoppingListId = newList.shoppingListId,
                    userId = userId,
                    ingredientId = ingredientId,
                    ingredientName = ingredientName,
                    ingredientCategory = category?.name,
                    isChecked = false
                )
                shoppingRepo.insertItem(item)
                val listTitle = newList.title.ifEmpty { newList.shoppingListId }
                onAddedDirectly?.invoke(listTitle)
            } else if (allLists.size == 1) {
                val singleList = allLists.first()
                val item = ShoppingListItemEntity(
                    shoppingListId = singleList.shoppingListId,
                    userId = userId,
                    ingredientId = ingredientId,
                    ingredientName = ingredientName,
                    ingredientCategory = category?.name,
                    isChecked = false
                )
                shoppingRepo.insertItem(item)
                val listTitle = singleList.title.ifEmpty { singleList.shoppingListId }
                onAddedDirectly?.invoke(listTitle)
            } else {
                // > 1 lists and no default -> prompt selection dialog!
                _uiState.update {
                    it.copy(
                        addShoppingListState = it.addShoppingListState.copy(
                            showDialog = true,
                            pendingIngredient = AddToShoppingListTarget(ingredientId, ingredientName, category),
                            availableLists = allLists
                        )
                    )
                }
            }
        }
    }

    fun onDismissSelectShoppingListDialog() {
        _uiState.update {
            it.copy(
                addShoppingListState = it.addShoppingListState.copy(
                    showDialog = false,
                    pendingIngredient = null,
                    availableLists = emptyList(),
                    isNewListOptionSelected = false,
                    newListNameInput = "",
                    selectedListIndex = 0
                )
            )
        }
    }

    fun updateIsNewListOption(isSelected: Boolean) {
        _uiState.update { it.copy(addShoppingListState = it.addShoppingListState.copy(isNewListOptionSelected = isSelected)) }
    }

    fun updateNewShoppingListName(name: String) {
        _uiState.update { it.copy(addShoppingListState = it.addShoppingListState.copy(newListNameInput = name)) }
    }

    fun updateSelectedShoppingListIndex(index: Int) {
        _uiState.update { it.copy(addShoppingListState = it.addShoppingListState.copy(selectedListIndex = index)) }
    }

    fun confirmAddPendingIngredientToShoppingList(
        onSuccess: (ingredientName: String, listTitle: String) -> Unit
    ) {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
        val state = _uiState.value.addShoppingListState
        val pending = state.pendingIngredient ?: return
        val targetList = state.availableLists.getOrNull(state.selectedListIndex) ?: return

        viewModelScope.launch {
            val item = ShoppingListItemEntity(
                shoppingListId = targetList.shoppingListId,
                userId = userId,
                ingredientId = pending.ingredientId,
                ingredientName = pending.ingredientName,
                ingredientCategory = pending.category?.name,
                isChecked = false
            )
            shoppingRepo.insertItem(item)
            val name = pending.ingredientName
            val listTitle = targetList.title.ifEmpty { targetList.shoppingListId }
            onDismissSelectShoppingListDialog()
            onSuccess(name, listTitle)
        }
    }

    fun confirmAddPendingIngredientToNewShoppingList(
        onSuccess: (ingredientName: String, listTitle: String) -> Unit
    ) {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
        val state = _uiState.value.addShoppingListState
        val pending = state.pendingIngredient ?: return
        val newListName = state.newListNameInput

        viewModelScope.launch {
            val newList = shoppingRepo.createShoppingList(userId, newListName.trim().ifEmpty { null })
            val item = ShoppingListItemEntity(
                shoppingListId = newList.shoppingListId,
                userId = userId,
                ingredientId = pending.ingredientId,
                ingredientName = pending.ingredientName,
                ingredientCategory = pending.category?.name,
                isChecked = false
            )
            shoppingRepo.insertItem(item)
            val name = pending.ingredientName
            val listTitle = newList.title.ifEmpty { newList.shoppingListId }
            onDismissSelectShoppingListDialog()
            onSuccess(name, listTitle)
        }
    }
}

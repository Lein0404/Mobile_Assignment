package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.local.IngredientsDatabase
import com.example.foodieheal.ingredients.local.ShoppingListEntity
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.repo.ShoppingListRepository
import com.example.foodieheal.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = IngredientsDatabase.getInstance(application)
    private val shoppingRepo = ShoppingListRepository(database.shoppingListDao())
    private val ingredientsRepo = IngredientsRepository(database.ingredientsDao())
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
            val allUnits = try { ingredientsRepo.getUnits() } catch (_: Exception) { emptyList() }.associateBy { it.unitID }
            val allIngredientUnits = try { ingredientsRepo.getAllIngredientUnits() } catch (_: Exception) { emptyList() }

            val ingredientsMap = ingredients.associateBy { it.ingredientId }

            shoppingRepo.getShoppingList(currentUserId).collectLatest { entities ->
                val items = entities.map { entity ->
                    val ingredient = ingredientsMap[entity.ingredientId]
                    val category = ingredient?.ingredientCategory
                    
                    val unitsForIngredient = allIngredientUnits.filter { it.ingredientID == entity.ingredientId }
                    val summary = unitsForIngredient.joinToString(", ") { iu ->
                        val unit = allUnits[iu.unitID]
                        val qty = unit?.defaultQuantity?.toInt() ?: 0
                        val name = unit?.unitName ?: ""
                        "${iu.caloriesPerDefaultQuantity.toInt()}kcal/${qty}${name}"
                    }
                    
                    ShoppingListItem(entity, category, summary)
                }
                
                _uiState.update { 
                    it.copy(
                        items = items,
                        isLoading = false,
                        errorMessage = if (ingredients.isEmpty()) "Using local data only." else null
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

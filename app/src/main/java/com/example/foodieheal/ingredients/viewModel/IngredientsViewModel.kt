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
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IngredientsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = IngredientsDatabase.getInstance(application)
    private val repository = IngredientsRepository(database.ingredientsDao())
    private val shoppingRepo = ShoppingListRepository(database.shoppingListDao())

    private val _uiState = MutableStateFlow(IngredientsUiState())
    val uiState: StateFlow<IngredientsUiState> = _uiState.asStateFlow()

    init {
        fetchIngredients()
    }

    fun fetchIngredients() {
        viewModelScope.launch {
            updateLoading(true)
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

                _uiState.update { it.copy(ingredients = ingredientItems, errorMessage = null) }
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = "Failed to fetch ingredients") }
            } finally {
                updateLoading(false)
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

    fun fetchIngredientDetail(id: String) {
        viewModelScope.launch {
            updateLoading(true)
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
                    _uiState.update { it.copy(ingredientDetail = IngredientDetailInfo(ingredient, calorieEntries, calorieSummary)) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = "Failed to fetch ingredient details") }
            } finally {
                updateLoading(false)
            }
        }
    }

    fun addToShoppingList(ingredient: Ingredients) {
        viewModelScope.launch {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
            if (userId.isEmpty()) return@launch

            val entity = ShoppingListEntity(
                userId = userId,
                ingredientId = ingredient.ingredientId,
                ingredientName = ingredient.ingredientName,
                isChecked = false
            )
            shoppingRepo.insertItem(entity)
        }
    }
}


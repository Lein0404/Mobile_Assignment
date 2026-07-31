package com.example.foodieheal.ingredients.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IngredientsViewModel(
    private val repository: IngredientsRepository = IngredientsRepository()
) : ViewModel() {

    var searchQuery by mutableStateOf("")
    var selectedCategories by mutableStateOf(setOf<IngredientCategory>())
    
    private val _ingredients = MutableStateFlow<List<Ingredients>>(emptyList())
    val ingredients: StateFlow<List<Ingredients>> = _ingredients.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val filteredIngredients = combine(
        _ingredients, 
        snapshotFlow { searchQuery }, 
        snapshotFlow { selectedCategories }
    ) { list, query, categories ->
        list.filter { 
            (query.isEmpty() || it.ingredientName.contains(query, ignoreCase = true)) &&
            (categories.isEmpty() || it.ingredientCategory == null || categories.contains(it.ingredientCategory))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchIngredients()
    }

    fun fetchIngredients() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _ingredients.value = repository.getIngredients()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCategory(category: IngredientCategory) {
        selectedCategories = if (selectedCategories.contains(category)) {
            selectedCategories - category
        } else {
            selectedCategories + category
        }
    }

    // Detail Screen logic
    private val _ingredientDetail = MutableStateFlow<IngredientDetailInfo?>(null)
    val ingredientDetail: StateFlow<IngredientDetailInfo?> = _ingredientDetail.asStateFlow()

    fun fetchIngredientDetail(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
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
                                unitName = unit.unitName
                            )
                        }
                    }
                    _ingredientDetail.value = IngredientDetailInfo(ingredient, calorieEntries)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

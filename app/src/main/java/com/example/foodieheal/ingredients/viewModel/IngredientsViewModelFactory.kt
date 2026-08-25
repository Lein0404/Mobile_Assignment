package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodieheal.ingredients.local.IngredientsDatabase
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.ingredients.repo.ShoppingListRepository

/**
 * `IngredientsViewModelFactory` A unified factory that
 * centralizes dependency creation and ensures that IngredientsDatabase remains a single instance
 */
class IngredientsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    private val database by lazy { IngredientsDatabase.getInstance(application) }
    private val ingredientsRepo by lazy { IngredientsRepository(database.ingredientsDao()) }
    private val shoppingRepo by lazy { ShoppingListRepository(database.shoppingListDao()) }
    private val requestRepo by lazy { IngredientRequestRepository() }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(IngredientsViewModel::class.java) -> {
                IngredientsViewModel(application, ingredientsRepo, shoppingRepo) as T
            }
            modelClass.isAssignableFrom(ShoppingListViewModel::class.java) -> {
                ShoppingListViewModel(application, shoppingRepo, ingredientsRepo) as T
            }
            modelClass.isAssignableFrom(IngredientRequestViewModel::class.java) -> {
                IngredientRequestViewModel(application, requestRepo) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

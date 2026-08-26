package com.example.foodieheal.Admin.ViewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodieheal.User.Repo.UserRepository
import com.example.foodieheal.ingredients.local.IngredientsDatabase
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository
import com.example.foodieheal.ingredients.repo.IngredientsRepository

class AdminViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    private val database by lazy { IngredientsDatabase.getInstance(application) }
    private val ingredientRequestRepo by lazy { IngredientRequestRepository() }
    private val userRepository by lazy { UserRepository() }
    private val ingredientsRepo by lazy { IngredientsRepository(database.ingredientsDao()) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AdminIngredientsViewModel::class.java) -> {
                AdminIngredientsViewModel(application, ingredientRequestRepo, userRepository) as T
            }
            modelClass.isAssignableFrom(AdminIngredientRequestViewModel::class.java) -> {
                AdminIngredientRequestViewModel(application, ingredientRequestRepo, userRepository, ingredientsRepo) as T
            }
            modelClass.isAssignableFrom(AdminAddIngredientViewModel::class.java) -> {
                AdminAddIngredientViewModel(application, ingredientsRepo) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

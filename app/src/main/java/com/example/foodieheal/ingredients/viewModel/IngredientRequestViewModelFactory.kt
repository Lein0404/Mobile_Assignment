package com.example.foodieheal.ingredients.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodieheal.ingredients.repo.IngredientRequestRepository

class IngredientRequestViewModelFactory(
    private val application: Application,
    private val repository: IngredientRequestRepository = IngredientRequestRepository(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IngredientRequestViewModel::class.java)) {
            return IngredientRequestViewModel(application, repository) as T
        }
        // You can add more ViewModels here in the future if they share the same repository
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

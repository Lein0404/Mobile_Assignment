package com.example.foodieheal.meal_planner.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import io.github.jan.supabase.postgrest.postgrest

class MealPlannerViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealPlannerViewModel::class.java)) {
            val repository = MealPlannerRepository(
                postgrest = SupabaseClient.client.postgrest,
                supabaseClient = SupabaseClient.client
            )
            return MealPlannerViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
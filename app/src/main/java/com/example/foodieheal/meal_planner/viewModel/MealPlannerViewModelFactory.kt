package com.example.foodieheal.meal_planner.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.local.HiringDatabase
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.meal_planner.local.MealPlanDatabase
import com.example.foodieheal.Recipe.local.RecipeDatabase
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import io.github.jan.supabase.postgrest.postgrest

class MealPlannerViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealPlannerViewModel::class.java)) {
            val localDao = MealPlanDatabase.getDatabase(application).mealPlanDao()
            val recipeDao = RecipeDatabase.getDatabase(application).recipeDao()
            val repository = MealPlannerRepository(
                postgrest = SupabaseClient.client.postgrest,
                supabaseClient = SupabaseClient.client,
                localDao = localDao,
                recipeDao = recipeDao
            )
            val recipeRepository = RecipeRepository(recipeDao)

            val hiringDb = HiringDatabase.getInstance(application)
            val hiringRepository = HiringRepository(
                chefDao = hiringDb.chefDao(),
                appointmentDao = hiringDb.appointmentDao(),
                reviewDao = hiringDb.chefReviewDao()
            )

            return MealPlannerViewModel(application, repository, recipeRepository, hiringRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

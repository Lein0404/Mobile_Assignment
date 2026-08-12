package com.example.foodieheal.meal_planner.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.MainActivity
import com.example.foodieheal.database.AppDatabase
import com.example.foodieheal.database.RecipeEntity
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.model.Recipe
import com.example.foodieheal.model.IngredientItem
import com.example.foodieheal.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

class TemplateViewModel(
    private val planRepository: PlanRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private fun getRecipeDao() = MainActivity.appContext?.let {
        AppDatabase.getDatabase(it).recipeDao()
    }

    /**
     * Exposes a hot UI StateFlow stream containing completely populated meal cards.
     */
    val allWeeklyPlans: StateFlow<List<WeeklyPlan>> = planRepository.observeAllPlans()
        .mapLatest { entityList ->
            entityList.map { entity ->
                WeeklyPlan(
                    planName = entity.planName,
                    planId = entity.planId,
                    userId = entity.userId,
                    category = entity.category,
                    weekStartDate = try {
                        LocalDate.parse(entity.weekStartDateString)
                    } catch (e: Exception) {
                        LocalDate.now()
                    },
                    dailyPlans = hydrateDailyPlans(entity.dailyPlans)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private suspend fun hydrateDailyPlans(
        rawDailyPlans: Map<String, List<String>>
    ): Map<LocalDate, List<Recipe>> = withContext(Dispatchers.IO) {
        val dao = getRecipeDao()

        rawDailyPlans.entries.associate { (dateStr, recipeIds) ->
            val parsedDate = try { LocalDate.parse(dateStr) } catch(e: Exception) { LocalDate.now() }

            val populatedRecipes = recipeIds.map { recipeId ->
                // 1. Check local room cache
                val localEntity = dao?.getRecipeById(recipeId)
                if (localEntity != null) {
                    mapEntityToRecipe(localEntity)
                } else {
                    // 2. Fallback safely using explicit defaults to satisfy non-null models
                    var fallbackRecipe = Recipe(
                        recipe_id = recipeId,
                        recipeName = "Loading...",
                        recipeDescription = "",
                        recipeCourse = "",
                        time = 0,
                        calories = 0,
                        cookingSkill = "",
                        estimatedBudget = "",
                        recipeStep = "",
                        ingredients = emptyList()
                    )

                    // Fire background network hydration call
                    recipeRepository.getRecipeById(recipeId).onSuccess { networkRecipe ->
                        fallbackRecipe = networkRecipe
                    }
                    fallbackRecipe
                }
            }
            parsedDate to populatedRecipes
        }
    }

    private fun mapEntityToRecipe(entity: RecipeEntity): Recipe {
        return Recipe(
            recipe_id = entity.recipe_id,
            author_id = entity.author_id,
            recipeName = entity.recipeName,
            recipeDescription = entity.recipeDescription,
            recipeCourse = entity.recipeCourse,
            time = entity.time,
            calories = entity.calories,
            cookingSkill = entity.cookingSkill,
            estimatedBudget = entity.estimatedBudget,
            recipeStep = entity.recipeStep,
            recipeImageUrl = entity.recipeImageUrl,
            ingredients = try {
                json.decodeFromString<List<IngredientItem>>(entity.ingredientsJson)
            } catch (e: Exception) { emptyList() }
        )
    }
}
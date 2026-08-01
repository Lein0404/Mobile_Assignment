package com.example.foodieheal.ingredients.repo

import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.model.IngredientUnits
import com.example.foodieheal.ingredients.model.Ingredients
import com.example.foodieheal.ingredients.model.Units
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fetches data from Supabase tables: ingredients, units, and ingredient_units.
 *
 */
class IngredientsRepository {
    suspend fun getIngredients(): List<Ingredients> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredients").select().decodeList<Ingredients>()
    }

    suspend fun getIngredientById(id: String): Ingredients? = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredients").select {
            filter {
                eq("ingredient_id", id)
            }
        }.decodeSingleOrNull<Ingredients>()
    }

    suspend fun getIngredientUnits(ingredientId: String): List<IngredientUnits> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredient_units").select {
            filter {
                eq("ingredient_id", ingredientId)
            }
        }.decodeList<IngredientUnits>()
    }

    suspend fun getAllIngredientUnits(): List<IngredientUnits> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredient_units").select().decodeList<IngredientUnits>()
    }

    suspend fun getUnits(): List<Units> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("units").select().decodeList<Units>()
    }
}

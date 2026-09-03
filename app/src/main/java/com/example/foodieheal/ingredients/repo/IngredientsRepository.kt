package com.example.foodieheal.ingredients.repo

import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.local.IngredientsDao
import com.example.foodieheal.ingredients.local.toDomain
import com.example.foodieheal.ingredients.local.toEntity
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.shared.IngredientNameNormalizer
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline-first repository for ingredients data.
 *
 * Strategy:
 * 1. Return cached data from Room immediately.
 * 2. Attempt to fetch fresh data from Supabase in the background.
 * 3. On success --> update Room cache and return fresh data.
 * 4. On failure (offline) --> keep using cached data silently.
 */
class IngredientsRepository(private val dao: IngredientsDao) {

    // ──────────────── Ingredients ────────────────

    suspend fun getIngredients(): List<Ingredients> = withContext(Dispatchers.IO) {
        // 1. Load from local cache first
        val cached = dao.getAllIngredients().map { it.toDomain() }

        // 2. Attempt remote fetch and update cache
        try {
            val remote = SupabaseClient.client.from("ingredients").select().decodeList<Ingredients>()
            dao.clearIngredients()
            dao.insertAllIngredients(remote.map { it.toEntity() })
            remote
        } catch (_: Exception) {
            // Offline or error — return cached data
            cached
        }
    }

    suspend fun getIngredientById(id: String): Ingredients? = withContext(Dispatchers.IO) {
        // 1. Load from local cache first
        val cached = dao.getIngredientById(id)?.toDomain()

        // 2. Attempt remote fetch
        try {
            val remote = SupabaseClient.client.from("ingredients").select {
                filter { eq("ingredient_id", id) }
            }.decodeSingleOrNull<Ingredients>()
            // Update cache if found
            if (remote != null) {
                dao.insertAllIngredients(listOf(remote.toEntity()))
            }
            remote ?: cached
        } catch (_: Exception) {
            cached
        }
    }

    // ──────────────── Ingredient Units ────────────────

    suspend fun getIngredientUnits(ingredientId: String): List<IngredientUnits> = withContext(Dispatchers.IO) {
        val cached = dao.getIngredientUnitsById(ingredientId).map { it.toDomain() }

        try {
            val remote = SupabaseClient.client.from("ingredient_units").select {
                filter { eq("ingredient_id", ingredientId) }
            }.decodeList<IngredientUnits>()
            dao.insertAllIngredientUnits(remote.map { it.toEntity() })
            remote
        } catch (_: Exception) {
            cached
        }
    }

    suspend fun getAllIngredientUnits(): List<IngredientUnits> = withContext(Dispatchers.IO) {
        val cached = dao.getAllIngredientUnits().map { it.toDomain() }

        try {
            val remote = SupabaseClient.client.from("ingredient_units").select().decodeList<IngredientUnits>()
            dao.clearIngredientUnits()
            dao.insertAllIngredientUnits(remote.map { it.toEntity() })
            remote
        } catch (_: Exception) {
            cached
        }
    }

    // ──────────────── Units ────────────────

    suspend fun getUnits(): List<Units> = withContext(Dispatchers.IO) {
        val cached = dao.getAllUnits().map { it.toDomain() }

        try {
            val remote = SupabaseClient.client.from("units").select().decodeList<Units>()
            dao.clearUnits()
            dao.insertAllUnits(remote.map { it.toEntity() })
            remote
        } catch (_: Exception) {
            cached
        }
    }

    suspend fun insertIngredient(ingredient: Ingredients) = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredients").insert(ingredient)
        dao.insertAllIngredients(listOf(ingredient.toEntity()))
    }

    suspend fun insertIngredientUnits(units: List<IngredientUnits>) = withContext(Dispatchers.IO) {
        if (units.isNotEmpty()) {
            SupabaseClient.client.from("ingredient_units").insert(units)
            dao.insertAllIngredientUnits(units.map { it.toEntity() })
        }
    }

    suspend fun getNextIngredientId(): String = withContext(Dispatchers.IO) {
        val existing = SupabaseClient.client.from("ingredients")
            .select { order("ingredient_id", Order.DESCENDING) }
            .decodeList<Ingredients>()

        val maxNum = existing
            .mapNotNull { it.ingredientId.removePrefix("ING").toIntOrNull() }
            .maxOrNull() ?: 0

        "ING${(maxNum + 1).toString().padStart(4, '0')}"
    }

    suspend fun getNextIngredientUnitIds(count: Int): List<String> = withContext(Dispatchers.IO) {
        val existing = SupabaseClient.client.from("ingredient_units")
            .select { order("ingredient_unit_id", Order.DESCENDING) }
            .decodeList<IngredientUnits>()

        val maxNum = existing
            .mapNotNull { it.ingredientUnitId.removePrefix("IGU").toIntOrNull() }
            .maxOrNull() ?: 0

        (1..count).map { i ->
            "IGU${(maxNum + i).toString().padStart(4, '0')}"
        }
    }

    suspend fun findExistingIngredientName(name: String): String? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null
        
        val allIngredients = getIngredients()
        allIngredients.firstOrNull {
            IngredientNameNormalizer.isMatch(it.ingredientName, name)
        }?.ingredientName
    }
}


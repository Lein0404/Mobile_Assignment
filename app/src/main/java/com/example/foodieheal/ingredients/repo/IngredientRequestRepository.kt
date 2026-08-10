package com.example.foodieheal.ingredients.repo

import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.model.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IngredientRequestRepository {

    suspend fun getIngredientRequests(userId: String): List<IngredientRequest> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredient_request")
            .select { filter { eq("user_id", userId) } }
            .decodeList<IngredientRequest>()
    }

    suspend fun getIngredientUnitsRequests(): List<IngredientUnitsRequest> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredient_units_request").select().decodeList<IngredientUnitsRequest>()
    }

    suspend fun getUnits(): List<Units> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("units").select().decodeList<Units>()
    }

    suspend fun getIngredientRequestById(requestId: String): IngredientRequest? = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredient_request")
            .select { filter { eq("ingredient_request_id", requestId) } }
            .decodeSingleOrNull<IngredientRequest>()
    }

    suspend fun getIngredientUnitsRequestsById(requestId: String): List<IngredientUnitsRequest> = withContext(Dispatchers.IO) {
        SupabaseClient.client.from("ingredient_units_request")
            .select { filter { eq("ingredient_request_id", requestId) } }
            .decodeList<IngredientUnitsRequest>()
    }

    suspend fun deleteIngredientRequest(requestId: String) = withContext(Dispatchers.IO) {
        // Delete associated units first
        SupabaseClient.client.from("ingredient_units_request")
            .delete { filter { eq("ingredient_request_id", requestId) } }
        
        // Then delete the main request
        SupabaseClient.client.from("ingredient_request")
            .delete { filter { eq("ingredient_request_id", requestId) } }
    }

    /**
     * Generates the next sequential ID for ingredient_request (INGR0001, INGR0002, ...).
     */
    suspend fun getNextRequestId(): String = withContext(Dispatchers.IO) {
        val existing = SupabaseClient.client.from("ingredient_request")
            .select { order("ingredient_request_id", Order.DESCENDING) }
            .decodeList<IngredientRequest>()

        val maxNum = existing
            .mapNotNull { it.ingredientRequestId.removePrefix("INGR").toIntOrNull() }
            .maxOrNull() ?: 0

        "INGR${(maxNum + 1).toString().padStart(4, '0')}"
    }

    /**
     * Generates the next sequential ID for ingredient_units_request (IGUR0001, IGUR0002, ...).
     * Returns a block of [count] sequential IDs.
     */
    suspend fun getNextUnitRequestIds(count: Int): List<String> = withContext(Dispatchers.IO) {
        val existing = SupabaseClient.client.from("ingredient_units_request")
            .select { order("ingredient_units_request_id", Order.DESCENDING) }
            .decodeList<IngredientUnitsRequest>()

        val maxNum = existing
            .mapNotNull { it.ingredientUnitsRequestId.removePrefix("IGUR").toIntOrNull() }
            .maxOrNull() ?: 0

        (1..count).map { i ->
            "IGUR${(maxNum + i).toString().padStart(4, '0')}"
        }
    }

    suspend fun submitIngredientRequest(
        request: IngredientRequest,
        unitRequests: List<IngredientUnitsRequest>
    ) = withContext(Dispatchers.IO) {
        // 1. Insert the main request
        SupabaseClient.client.from("ingredient_request").insert(request)

        // 2. Insert associated unit requests
        if (unitRequests.isNotEmpty()) {
            SupabaseClient.client.from("ingredient_units_request").insert(unitRequests)
        }
    }

    suspend fun updateIngredientRequest(
        request: IngredientRequest,
        unitRequests: List<IngredientUnitsRequest>
    ) = withContext(Dispatchers.IO) {
        // 1. Update the main request (excluding datetime_created)
        SupabaseClient.client.from("ingredient_request").update(
            {
                set("ing_name", request.ingredientName)
                set("ing_category", request.ingredientCategory)
                set("ing_description", request.ingredientDesc)
                set("ing_image", request.ingredientImage)
                set("request_status", request.requestStatus)
            }
        ) {
            filter {
                eq("ingredient_request_id", request.ingredientRequestId)
            }
        }

        // 2. Refresh associated unit requests
        // Delete old units
        SupabaseClient.client.from("ingredient_units_request")
            .delete { filter { eq("ingredient_request_id", request.ingredientRequestId) } }
        
        // Insert new units
        if (unitRequests.isNotEmpty()) {
            SupabaseClient.client.from("ingredient_units_request").insert(unitRequests)
        }
    }
}


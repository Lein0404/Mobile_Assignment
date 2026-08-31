package com.example.foodieheal.ingredients.notification

import android.content.Context
import android.util.Log
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.model.IngredientRequest
import com.example.foodieheal.model.Status
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Monitors the status of ingredient requests and triggers notifications when they are approved or rejected.
 */
object IngredientRequestStatusMonitor {

    private const val TAG = "StatusMonitor"
    private const val PREFS_NAME = "ingredient_request_notifications"
    private const val KEY_INITIALIZED = "has_initialized_request_tracking"
    private const val KEY_PREFIX_STATUS = "notified_status_"

    /**
     * Checks Supabase directly for status updates of the given user's ingredient requests
     * and triggers notifications for any newly resolved requests.
     */
    suspend fun checkStatusUpdates(userId: String, context: Context) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            val requests = SupabaseClient.client.from("ingredient_request")
                .select { filter { eq("user_id", userId) } }
                .decodeList<IngredientRequest>()

            processRequestList(requests, context)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ingredient request status updates: ${e.message}", e)
        }
    }

    /**
     * Processes a list of ingredient requests and dispatches notifications for newly APPROVED or REJECTED items.
     */
    fun processRequestList(requests: List<IngredientRequest>, context: Context) {
        if (requests.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasInitialized = prefs.getBoolean(KEY_INITIALIZED, false)

        if (!hasInitialized) {
            // First time tracking: record all existing statuses so we don't spam old notifications
            val editor = prefs.edit()
            requests.forEach { req ->
                editor.putString("$KEY_PREFIX_STATUS${req.ingredientRequestId}", req.requestStatus.name)
            }
            editor.putBoolean(KEY_INITIALIZED, true)
            editor.apply()
            Log.d(TAG, "Initialized ingredient request tracking with ${requests.size} existing requests.")
            return
        }

        // On subsequent checks: inspect each request for newly resolved status
        val editor = prefs.edit()
        requests.forEach { req ->
            val prevStatus = prefs.getString("$KEY_PREFIX_STATUS${req.ingredientRequestId}", null)
            val currentStatus = req.requestStatus

            if ((currentStatus == Status.APPROVED || currentStatus == Status.REJECTED) &&
                prevStatus != currentStatus.name
            ) {
                Log.d(TAG, "Request ${req.ingredientRequestId} status changed: $prevStatus -> ${currentStatus.name}. Posting notification.")
                IngredientRequestNotificationHelper.showRequestStatusNotification(
                    context = context,
                    requestId = req.ingredientRequestId,
                    ingredientName = req.ingredientName,
                    status = currentStatus
                )
                editor.putString("$KEY_PREFIX_STATUS${req.ingredientRequestId}", currentStatus.name)
            } else if (prevStatus == null) {
                // Record newly created pending requests
                editor.putString("$KEY_PREFIX_STATUS${req.ingredientRequestId}", currentStatus.name)
            }
        }
        editor.apply()
    }
}

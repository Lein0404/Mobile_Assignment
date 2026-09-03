package com.example.foodieheal.ingredients.notification

import android.content.Context
import android.util.Log
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.ingredients.model.IngredientRequest
import com.example.foodieheal.model.Status
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Monitors the status of ingredient requests and triggers notifications when they are approved or rejected.
 */
object IngredientRequestStatusMonitor {

    private const val TAG = "StatusMonitor"
    private const val PREFS_NAME = "ingredient_request_notifications"
    private const val GLOBAL_PREFS = "ingredient_request_global_prefs"
    private const val KEY_LAST_ACTIVE_USER_ID = "last_active_user_id"
    private const val KEY_PREFIX_STATUS = "notified_status_"

    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    /**
     * Starts continuous background polling at the process level.
     * Continues running even when activities are paused, stopped, or minimized.
     */
    fun startPolling(userId: String, context: Context, intervalMs: Long = 5000L) {
        if (userId.isBlank()) return
        saveActiveUserId(userId, context)

        // Cancel previous polling job if any
        pollingJob?.cancel()

        pollingJob = monitorScope.launch {
            Log.d(TAG, "Started process-level status polling for user '$userId' (Interval: ${intervalMs}ms)")
            while (isActive) {
                try {
                    checkStatusUpdates(userId, context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during process-level polling cycle: ${e.message}", e)
                }
                delay(intervalMs)
            }
        }
    }

    /**
     * Stops continuous process-level polling (e.g., when the user logs out).
     */
    fun stopPolling(context: Context? = null) {
        pollingJob?.cancel()
        pollingJob = null
        if (context != null) {
            clearActiveUserId(context)
        }
        Log.d(TAG, "Stopped process-level status polling.")
    }

    /**
     * Persists the active user ID so WorkManager background tasks can retrieve it even in a dead process.
     */
    fun saveActiveUserId(userId: String, context: Context) {
        if (userId.isBlank()) return
        val prefs = context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_ACTIVE_USER_ID, userId).apply()
    }

    /**
     * Retrieves the persisted active user ID.
     */
    fun getActiveUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_ACTIVE_USER_ID, null)
    }

    /**
     * Clears the persisted active user ID (on user logout).
     */
    fun clearActiveUserId(context: Context) {
        val prefs = context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LAST_ACTIVE_USER_ID).apply()
    }

    /**
     * Records a newly submitted request as PENDING immediately so that when it is later
     * resolved (approved/rejected), the status transition is guaranteed to trigger an alert.
     */
    fun recordPendingRequest(userId: String, requestId: String, context: Context) {
        if (userId.isBlank() || requestId.isBlank()) return
        val prefs = context.getSharedPreferences("${PREFS_NAME}_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_PREFIX_STATUS$requestId", Status.PENDING.name).apply()
        Log.d(TAG, "Recorded initial PENDING tracking for request $requestId (user: $userId)")
    }

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

            Log.d(TAG, "Fetched ${requests.size} requests for user: $userId")
            processRequestList(userId, requests, context)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ingredient request status updates for user $userId: ${e.message}", e)
        }
    }

    /**
     * Processes a list of ingredient requests and dispatches notifications for newly APPROVED or REJECTED items.
     */
    fun processRequestList(userId: String, requests: List<IngredientRequest>, context: Context) {
        if (requests.isEmpty()) return

        val prefsName = if (userId.isNotBlank()) "${PREFS_NAME}_$userId" else PREFS_NAME
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        requests.forEach { req ->
            val prevStatus = prefs.getString("$KEY_PREFIX_STATUS${req.ingredientRequestId}", null)
            val currentStatus = req.requestStatus

            Log.d(TAG, "Checking request ${req.ingredientRequestId} (${req.ingredientName}): prevStatus=$prevStatus, currentStatus=${currentStatus.name}")

            when {
                // Case 1: Status is APPROVED or REJECTED and different from previously recorded status
                (currentStatus == Status.APPROVED || currentStatus == Status.REJECTED) && prevStatus != currentStatus.name -> {
                    // If prevStatus is null (first time seeing this completed request), check if it was processed recently
                    val shouldNotify = if (prevStatus == null) {
                        isProcessedRecently(req.datetimeProcessed)
                    } else {
                        true // Was previously PENDING or had a different status
                    }

                    if (shouldNotify) {
                        Log.d(TAG, "Status transition detected for ${req.ingredientRequestId}: $prevStatus -> ${currentStatus.name}. Posting notification.")
                        IngredientRequestNotificationHelper.showRequestStatusNotification(
                            context = context,
                            requestId = req.ingredientRequestId,
                            ingredientName = req.ingredientName,
                            status = currentStatus
                        )
                    } else {
                        Log.d(TAG, "Skipping notification for old completed request ${req.ingredientRequestId} (processed: ${req.datetimeProcessed})")
                    }
                    editor.putString("$KEY_PREFIX_STATUS${req.ingredientRequestId}", currentStatus.name)
                }

                // Case 2: New PENDING request discovered
                prevStatus == null && currentStatus == Status.PENDING -> {
                    editor.putString("$KEY_PREFIX_STATUS${req.ingredientRequestId}", currentStatus.name)
                }
            }
        }
        editor.apply()
    }

    private fun isProcessedRecently(datetimeProcessed: String?): Boolean {
        if (datetimeProcessed.isNullOrBlank()) return false
        return try {
            val processedInstant = try {
                Instant.parse(datetimeProcessed)
            } catch (_: Exception) {
                ZonedDateTime.parse(datetimeProcessed).toInstant()
            }
            val age = Duration.between(processedInstant, Instant.now())
            // Notify if processed within the last 6 hours
            !age.isNegative && age.toHours() < 6
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse datetimeProcessed '$datetimeProcessed': ${e.message}")
            false
        }
    }
}

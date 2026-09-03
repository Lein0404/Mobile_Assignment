package com.example.foodieheal.ingredients.notification

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.foodieheal.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.util.concurrent.TimeUnit

class IngredientRequestSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Retrieve userId from WorkManager input data, persistent storage, or Supabase auth session
            val userId = inputData.getString(KEY_USER_ID)
                ?: IngredientRequestStatusMonitor.getActiveUserId(applicationContext)
                ?: SupabaseClient.client.auth.currentUserOrNull()?.id

            if (!userId.isNullOrBlank()) {
                Log.d(TAG, "Running background status check for user: $userId")
                IngredientRequestStatusMonitor.checkStatusUpdates(userId, applicationContext)
            } else {
                Log.w(TAG, "Background sync skipped: No active user ID found.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during background status sync: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RequestSyncWorker"
        const val KEY_USER_ID = "user_id"
        private const val UNIQUE_WORK_NAME = "IngredientRequestSyncWork"

        fun enqueuePeriodicSync(context: Context, userId: String? = null) {
            val targetUserId = userId ?: IngredientRequestStatusMonitor.getActiveUserId(context)
            if (!targetUserId.isNullOrBlank()) {
                IngredientRequestStatusMonitor.saveActiveUserId(targetUserId, context)
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = if (!targetUserId.isNullOrBlank()) {
                workDataOf(KEY_USER_ID to targetUserId)
            } else {
                workDataOf()
            }

            // PeriodicWorkRequest = checks Supabase in the background whenever network is available (min 15 min interval)
            val periodicWork = PeriodicWorkRequestBuilder<IngredientRequestSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWork
            )
            Log.d(TAG, "Enqueued periodic status sync work for user: $targetUserId.")
        }

        fun enqueueImmediateSync(context: Context, userId: String? = null) {
            val targetUserId = userId ?: IngredientRequestStatusMonitor.getActiveUserId(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = if (!targetUserId.isNullOrBlank()) {
                workDataOf(KEY_USER_ID to targetUserId)
            } else {
                workDataOf()
            }

            val oneTimeWork = OneTimeWorkRequestBuilder<IngredientRequestSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ImmediateIngredientRequestSyncWork",
                ExistingWorkPolicy.REPLACE,
                oneTimeWork
            )
            Log.d(TAG, "Enqueued immediate status sync work for user: $targetUserId.")
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork("ImmediateIngredientRequestSyncWork")
            IngredientRequestStatusMonitor.clearActiveUserId(context)
            Log.d(TAG, "Cancelled periodic and immediate status sync work and cleared active user ID.")
        }
    }
}

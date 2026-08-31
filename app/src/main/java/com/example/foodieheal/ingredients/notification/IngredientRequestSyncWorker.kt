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
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (!userId.isNullOrBlank()) {
                Log.d(TAG, "Running background status check for user: $userId")
                IngredientRequestStatusMonitor.checkStatusUpdates(userId, applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during background status sync: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RequestSyncWorker"
        private const val UNIQUE_WORK_NAME = "IngredientRequestSyncWork"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // PeriodicWorkRequest = checks Supabase in the background whenever network is available
            val periodicWork = PeriodicWorkRequestBuilder<IngredientRequestSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            // WorkManager = performs background sync
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
            Log.d(TAG, "Enqueued periodic status sync work.")
        }

        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeWork = OneTimeWorkRequestBuilder<IngredientRequestSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ImmediateIngredientRequestSyncWork",
                ExistingWorkPolicy.REPLACE,
                oneTimeWork
            )
            Log.d(TAG, "Enqueued immediate status sync work.")
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork("ImmediateIngredientRequestSyncWork")
            Log.d(TAG, "Cancelled periodic and immediate status sync work.")
        }
    }
}

package com.example.foodieheal.ui.components

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

enum class CacheTarget {
    ALL,
    TEMP_FILES
}

/**
 * Multipurpose cache clean that detects when the user swipes away
 * or closes the application from the app switcher
 *
 * Clearing multiple Room databases
 * Purging temporary files in context.cacheDir.
 */
open class AppCacheCleanupService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "App task removed from App Switcher. Executing multipurpose cache cleanup...")

        val appContext = applicationContext
        runBlocking(Dispatchers.IO) {
            try {
                purgeAllCache(appContext)
                Log.d(TAG, "All cache targets successfully cleared on task removal.")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cache cleanup on task removed", e)
            }
        }
        stopSelf()
    }

    companion object {
        private const val TAG = "AppCacheCleanupService"

        // Dynamic cleanup handlers registered by other components
        private val customCleanupHandlers = ConcurrentHashMap<String, suspend (Context) -> Unit>()

        /**
         * Register a custom cleanup task for any Room database or cache.
         * Example:
         * AppCacheCleanupService.registerCleanupHandler("myModule") { context ->
         *     MyDatabase.getInstance(context).myDao().clearAll()
         * }
         */
        fun registerCleanupHandler(key: String, handler: suspend (Context) -> Unit) {
            customCleanupHandlers[key] = handler
            Log.d(TAG, "Registered custom cleanup handler: $key")
        }

        /**
         * Unregister a custom cleanup task.
         */
        fun unregisterCleanupHandler(key: String) {
            customCleanupHandlers.remove(key)
        }

        /**
         * Start the cleanup service so it listens for task removal.
         */
        fun start(context: Context) {
            try {
                val intent = Intent(context.applicationContext, AppCacheCleanupService::class.java)
                context.applicationContext.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AppCacheCleanupService", e)
            }
        }

        /**
         * Purges cache for specified targets or ALL.
         * NOTE: Room databases are no longer cleared automatically to support offline persistence.
         */
        suspend fun purgeCache(context: Context, targets: Set<CacheTarget> = setOf(CacheTarget.ALL)) {
            val shouldClearAll = targets.contains(CacheTarget.ALL)

            // Temporary cache files
            if (shouldClearAll || targets.contains(CacheTarget.TEMP_FILES)) {
                try {
                    context.cacheDir.listFiles()?.forEach { file ->
                        if (file.isFile) file.delete()
                    }
                    Log.d(TAG, "Cleared temporary cache files.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing temp files", e)
                }
            }

            // Run all dynamically registered handlers
            customCleanupHandlers.forEach { (key, handler) ->
                try {
                    handler(context)
                    Log.d(TAG, "Executed custom handler: $key")
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing custom cleanup handler: $key", e)
                }
            }
        }

        private suspend fun purgeAllCache(context: Context) {
            purgeCache(context, setOf(CacheTarget.ALL))
        }
    }
}

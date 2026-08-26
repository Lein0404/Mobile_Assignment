package com.example.foodieheal.hiring.local

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.foodieheal.Chef.local.ChefDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service that detects when the user swipe up / remove the app from the app switcher
 * When onTaskRemoved is triggered by the Android OS, it automatically clears the cached hiring and appointments data.
 */
class HiringCacheCleanupService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "User swiped up app in App Switcher. Purging hiring & appointment cache...")
        try {
            // Delete sentinel markers so any cache detection instantly triggers a purge
            val hiringSentinel = java.io.File(applicationContext.cacheDir, HiringDatabase.SENTINEL_FILE_NAME)
            if (hiringSentinel.exists()) hiringSentinel.delete()

            val chefSentinel = java.io.File(applicationContext.cacheDir, ChefDatabase.SENTINEL_FILE_NAME)
            if (chefSentinel.exists()) chefSentinel.delete()

            val hiringDb = HiringDatabase.getInstance(applicationContext)
            val chefDb = ChefDatabase.getInstance(applicationContext)

            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                try {
                    hiringDb.appointmentDao().clearAppointments()
                    hiringDb.chefDao().clearChefs()
                    hiringDb.chefBookmarkDao().clearAllBookmarks()
                    hiringDb.chefReviewDao().clearAllReviews()
                    Log.d(TAG, "Hiring database cache (including appointments) successfully purged.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error purging hiring cache on task removed", e)
                }

                try {
                    chefDb.chefPortalAppointmentDao().clearAllAppointments()
                    chefDb.chefPortalUserDao().clearAllUsers()
                    Log.d(TAG, "Chef database cache successfully purged on task removed.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error purging chef cache on task removed", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in HiringCacheCleanupService onTaskRemoved", e)
        }
        stopSelf()
    }

    companion object {
        private const val TAG = "HiringCacheCleanupService"
    }
}

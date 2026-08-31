package com.example.foodieheal.ingredients.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.foodieheal.MainActivity
import com.example.foodieheal.R
import com.example.foodieheal.model.Status
import com.example.foodieheal.navigation.Screen

object IngredientRequestNotificationHelper {

    private const val TAG = "IngredientReqNotif"
    const val CHANNEL_ID = "ingredient_requests_channel"

    /**
     * Safe to call repeatedly because creating an existing channel with the same ID is a no-op.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.ingredient_request_channel_name)
            val descriptionText = context.getString(R.string.ingredient_request_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a local push notification informing the user that their ingredient request was approved or rejected.
     */
    fun showRequestStatusNotification(
        context: Context,
        requestId: String,
        ingredientName: String,
        status: Status
    ) {
        if (status != Status.APPROVED && status != Status.REJECTED) return

        // Validate POST_NOTIFICATIONS runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping notification.")
                return
            }
        }

        try {
            createNotificationChannel(context)

            val targetRoute = Screen.IngredientDetail.createRoute(id = requestId, isRequest = true)

            // Intent to bring MainActivity to foreground and navigate to the request detail screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", targetRoute)
            }

            // Use requestId.hashCode() for unique requestCode & notificationId
            val notificationId = (5000 + (requestId.hashCode() % 5000)).let { if (it < 0) -it else it }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (status == Status.APPROVED) {
                context.getString(R.string.ingredient_request_approved_title)
            } else {
                context.getString(R.string.ingredient_request_rejected_title)
            }

            val message = if (status == Status.APPROVED) {
                context.getString(R.string.ingredient_request_approved_msg, ingredientName)
            } else {
                context.getString(R.string.ingredient_request_rejected_msg, ingredientName)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.foodieheallogo_removebg_preview)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Successfully posted request status notification for request: $requestId (Status: $status)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error displaying notification: ${e.message}", e)
        }
    }
}

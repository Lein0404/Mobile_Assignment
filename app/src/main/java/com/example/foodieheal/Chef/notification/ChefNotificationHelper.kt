package com.example.foodieheal.Chef.notification

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

object ChefNotificationHelper {

    private const val TAG = "ChefNotificationHelper"
    const val CHANNEL_ID = "chef_appointments_channel"
    private const val CHANNEL_NAME = "Chef Booking Alerts"
    private const val CHANNEL_DESCRIPTION = "Notifications for new pending client appointments and booking requests"
    const val NOTIFICATION_ID = 4001

    // Safe to call repeatedly because creating an existing channel with the same ID is a no-op.
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    // Posts a local push notification informing the chef of pending appointment bookings.
    fun showPendingAppointmentNotification(
        context: Context,
        pendingCount: Int,
        clientName: String? = null
    ) {
        if (pendingCount <= 0) {
            cancelNotification(context)
            return
        }

        // Validate post notification runtime permission
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

            // Intent to bring MainActivity to the foreground
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (pendingCount == 1) {
                "New Booking Request"
            } else {
                "$pendingCount Pending Booking Requests"
            }

            val message = if (!clientName.isNullOrBlank()) {
                if (pendingCount == 1) {
                    "$clientName requested a booking. Tap to review and confirm."
                } else {
                    "$clientName and ${pendingCount - 1} other client(s) requested bookings. Tap to review."
                }
            } else {
                "You have $pendingCount pending appointment${if (pendingCount > 1) "s" else ""} waiting for your response."
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.foodieheallogo_removebg_preview)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setNumber(pendingCount)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Successfully posted pending appointment notification ($pendingCount pending).")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error displaying notification: ${e.message}", e)
        }
    }

    const val CONFIRMED_NOTIFICATION_ID = 4002

    // Posts a push notification informing the chef that an appointment has been paid and confirmed.
    fun showConfirmedAppointmentNotification(
        context: Context,
        clientName: String? = null,
        appointmentDate: String? = null,
        appointmentTime: String? = null,
        totalAmount: Double? = null,
        appointmentId: String? = null
    ) {
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

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO", "chef_appointments")
                if (!appointmentId.isNullOrBlank()) {
                    putExtra("APPOINTMENT_ID", appointmentId)
                }
            }

            val requestCode = appointmentId?.hashCode() ?: CONFIRMED_NOTIFICATION_ID
            val pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = context.getString(R.string.chef_notif_confirmed_title)
            val formattedAmount = totalAmount?.let { String.format(java.util.Locale.US, "RM %.2f", it) }

            val message = when {
                !clientName.isNullOrBlank() && !appointmentDate.isNullOrBlank() && formattedAmount != null -> {
                    context.getString(R.string.chef_notif_confirmed_msg_full, clientName, formattedAmount, appointmentDate)
                }
                !clientName.isNullOrBlank() && formattedAmount != null -> {
                    context.getString(R.string.chef_notif_confirmed_msg_amount, clientName, formattedAmount)
                }
                !clientName.isNullOrBlank() -> {
                    context.getString(R.string.chef_notif_confirmed_msg_client, clientName)
                }
                else -> {
                    context.getString(R.string.chef_notif_confirmed_msg_default)
                }
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.foodieheallogo_removebg_preview)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            val notifId = appointmentId?.hashCode() ?: CONFIRMED_NOTIFICATION_ID
            notificationManager.notify(notifId, notification)
            Log.d(TAG, "Successfully posted confirmed appointment notification for $appointmentId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing confirmed notification: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error displaying confirmed notification: ${e.message}", e)
        }
    }

    // Cancel the active pending appointments notification.
    fun cancelNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification: ${e.message}", e)
        }
    }
}

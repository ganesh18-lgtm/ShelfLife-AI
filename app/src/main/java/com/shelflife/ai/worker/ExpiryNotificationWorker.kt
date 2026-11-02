package com.shelflife.ai.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shelflife.ai.MainActivity
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Scheduled background worker that monitors pantry items and fires local system notifications
 * for food expiring within 48 hours to minimize food waste.
 */
@HiltWorker
class ExpiryNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PantryRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "com.shelflife.ai.worker.ExpiryNotificationWorker"
        const val CHANNEL_ID = "shelf_life_expiry_channel"
        const val CHANNEL_NAME = "ShelfLife Expiry Alerts"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val threshold = now + ExpiryUrgency.FORTY_EIGHT_HOURS_MILLIS

            val expiringItems = repository.getItemsExpiringWithinSync(threshold)
                .filter { it.isInStock }

            if (expiringItems.isNotEmpty()) {
                sendExpiryNotification(expiringItems, now)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun sendExpiryNotification(items: List<PantryItem>, now: Long) {
        val notificationManager = NotificationManagerCompat.from(appContext)

        // 1. Create notification channel for Android O+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily alerts for pantry items nearing their expiration date"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Intent to launch MainActivity when tapped
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val count = items.size
        val title = if (count == 1) {
            "1 item is expiring soon!"
        } else {
            "$count items are expiring soon!"
        }

        val summaryText = if (count == 1) {
            "${items.first().name} needs your attention."
        } else {
            "${items.first().name} and ${count - 1} other item(s) are close to expiring."
        }

        // 3. Build rich InboxStyle notification
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("Smart Pantry Alert")

        items.take(5).forEach { item ->
            val hoursRemaining = TimeUnit.MILLISECONDS.toHours(item.expiryTimestamp - now).coerceAtLeast(0)
            val timeText = if (hoursRemaining < 24) {
                "in ${hoursRemaining}h"
            } else {
                "in ${hoursRemaining / 24}d"
            }
            inboxStyle.addLine("${item.name} (qty: ${item.quantity}) — expires $timeText")
        }

        if (count > 5) {
            inboxStyle.addLine("+ ${count - 5} more item(s)")
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 4. Check notification permission on Android 13+ before notifying
        try {
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } catch (_: SecurityException) {
            // Handled safely if user revoked POST_NOTIFICATIONS
        }
    }
}

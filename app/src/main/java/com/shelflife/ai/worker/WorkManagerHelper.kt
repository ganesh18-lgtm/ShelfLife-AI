package com.shelflife.ai.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utility helper to schedule and manage periodic and one-time expiry checks via [WorkManager].
 */
object WorkManagerHelper {

    /**
     * Schedules a daily background job to scan for expiring items.
     * Uses [ExistingPeriodicWorkPolicy.KEEP] to prevent resetting an ongoing schedule.
     */
    fun scheduleDailyExpiryCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS // 1 hour flex window
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ExpiryNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * Enqueues an immediate one-time execution for instant testing or user-initiated sync.
     */
    fun triggerImmediateExpiryCheck(context: Context) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ExpiryNotificationWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${ExpiryNotificationWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }
}

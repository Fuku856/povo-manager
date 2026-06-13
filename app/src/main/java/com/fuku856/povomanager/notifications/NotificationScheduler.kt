package com.fuku856.povomanager.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** 毎日 notifyHour 時に期限チェックを実行するよう(再)スケジュールする */
    fun schedule(notifyHour: Int) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(notifyHour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(Duration.ofDays(1))
            .setInitialDelay(Duration.between(now, next))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ExpiryCheckWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

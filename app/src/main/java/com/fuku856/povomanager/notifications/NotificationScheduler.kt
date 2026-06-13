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
    /**
     * 毎日 notifyHour 時 notifyMinute 分に期限チェックを実行するようスケジュールする。
     *
     * @param replace true のとき既存スケジュールを置き換える(通知時刻の変更時のみ)。
     *   false(既定)では KEEP となり、既存スケジュールがあれば何もしない。
     *   起動のたびに UPDATE で置き換えると初期遅延がリセットされ、通知時刻直前/直後に
     *   起動した日の実行が翌日送りになって通知が飛ぶことがあるため、既定は KEEP とする。
     */
    fun schedule(notifyHour: Int, notifyMinute: Int = 0, replace: Boolean = false) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(notifyHour, notifyMinute)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(Duration.ofDays(1))
            .setInitialDelay(Duration.between(now, next))
            .build()

        val policy = if (replace) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ExpiryCheckWorker.UNIQUE_WORK_NAME,
            policy,
            request,
        )
    }
}

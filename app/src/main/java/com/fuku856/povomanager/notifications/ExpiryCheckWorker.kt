package com.fuku856.povomanager.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.shouldNotifyExpiry
import com.fuku856.povomanager.domain.toStatus
import com.fuku856.povomanager.domain.toppingsToNotify
import com.fuku856.povomanager.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/** 毎日1回、全回線の期限をチェックして通知を発行する */
@HiltWorker
class ExpiryCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: LineRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
    private val widgetUpdater: WidgetUpdater,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.current()
        val today = LocalDate.now()
        repository.getLinesWithPurchases().forEach { lineWithPurchases ->
            val status = lineWithPurchases.toStatus(settings, today)
            if (shouldNotifyExpiry(status, settings)) {
                notificationHelper.notifyExpiry(status)
            }
            notificationHelper.notifyToppings(
                line = lineWithPurchases.line,
                toppings = toppingsToNotify(lineWithPurchases.purchases, settings, today),
            )
        }
        // 日付の進行をウィジェットに反映
        widgetUpdater.updateAll()
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "expiry_check"
    }
}

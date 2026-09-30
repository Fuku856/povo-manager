package com.fuku856.povomanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.nextToppingAlertAt
import com.fuku856.povomanager.notifications.NotificationHelper
import com.fuku856.povomanager.notifications.NotificationScheduler
import com.fuku856.povomanager.notifications.ToppingAlarmScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltAndroidApp
class PovoManagerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationScheduler: NotificationScheduler
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var lineRepository: LineRepository
    @Inject lateinit var toppingAlarmScheduler: ToppingAlarmScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannels()
        appScope.launch {
            val settings = settingsRepository.current()
            notificationScheduler.schedule(settings.notifyHour, settings.notifyMinute)
        }
        appScope.launch {
            toppingAlarmScheduler.initLastCheckIfAbsent()
            // 購入の追加・編集・削除、インポート、アーカイブ、通知設定の変更のたびに
            // 「〜時間前」通知の次回アラームを設定し直す(変更の起点をここ1か所に集める)
            combine(
                lineRepository.observeActiveLinesWithPurchases(),
                settingsRepository.settings,
            ) { lines, settings ->
                nextToppingAlertAt(lines, settings, LocalDateTime.now())
            }.collect(toppingAlarmScheduler::schedule)
        }
    }
}

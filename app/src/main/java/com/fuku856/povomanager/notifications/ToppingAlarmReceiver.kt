package com.fuku856.povomanager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ToppingAlarmEntryPoint {
    fun toppingAlarmScheduler(): ToppingAlarmScheduler
}

/** 「〜時間前」通知のアラームを受けて、通知と次回アラームの設定を行う */
class ToppingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        checkToppingAlerts(context)
    }
}

/** 端末の再起動・アプリの更新でアラームが消えるため、その間に迎えた分の通知とアラームの再設定を行う */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        checkToppingAlerts(context)
    }
}

private fun BroadcastReceiver.checkToppingAlerts(context: Context) {
    val scheduler = EntryPointAccessors
        .fromApplication(context, ToppingAlarmEntryPoint::class.java)
        .toppingAlarmScheduler()
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            scheduler.checkAndReschedule()
        } finally {
            pendingResult.finish()
        }
    }
}

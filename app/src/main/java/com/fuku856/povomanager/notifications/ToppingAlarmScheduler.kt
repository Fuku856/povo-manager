package com.fuku856.povomanager.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.nextToppingAlertAt
import com.fuku856.povomanager.domain.toppingAlertsDue
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * トッピング有効期限の「〜時間前」通知のスケジューラ。
 * 日次の [ExpiryCheckWorker] では時間単位の通知ができないため、次に通知すべき時刻に
 * アラームを1つだけ設定し、鳴るたびに通知を出して次のアラームを設定し直す。
 */
@Singleton
class ToppingAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LineRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * 次回の通知アラームを [nextAt] に設定する(既存のアラームは置き換わる)。null なら解除する。
     */
    fun schedule(nextAt: LocalDateTime?) {
        val pendingIntent = alarmPendingIntent()
        if (nextAt == null) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val triggerAtMillis = nextAt.toEpochMillis()
        // 「1時間前」が数時間遅れては意味がないため、スリープ中でも時刻どおりに鳴る正確なアラームを使う。
        // 権限を取り消されている場合は、遅れることがある通常のアラームで代替する。
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * 前回確認した時刻から現在までに通知時刻を迎えたトッピングを通知し、次回のアラームを設定する。
     * アラーム受信時と、アラームが消える端末再起動・アプリ更新の後に呼ぶ。
     */
    suspend fun checkAndReschedule() {
        val now = LocalDateTime.now()
        val settings = settingsRepository.current()
        val lines = repository.getActiveLinesWithPurchases()
        val lastCheck = settingsRepository.toppingAlertLastCheck()?.toLocalDateTime() ?: now
        toppingAlertsDue(lines, settings, from = lastCheck, to = now).forEach { (line, toppings) ->
            notificationHelper.notifyToppingHours(line, toppings, now)
        }
        settingsRepository.setToppingAlertLastCheck(now.toEpochMillis())
        schedule(nextToppingAlertAt(lines, settings, now))
    }

    /**
     * 確認済み時刻が未記録なら現在時刻で初期化する。
     * 初回のアラームで、この機能を入れる前の分までまとめて通知しないようにするため。
     */
    suspend fun initLastCheckIfAbsent() {
        if (settingsRepository.toppingAlertLastCheck() == null) {
            settingsRepository.setToppingAlertLastCheck(LocalDateTime.now().toEpochMillis())
        }
    }

    private fun alarmPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ToppingAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun LocalDateTime.toEpochMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun Long.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

    private companion object {
        const val REQUEST_CODE = 1
    }
}

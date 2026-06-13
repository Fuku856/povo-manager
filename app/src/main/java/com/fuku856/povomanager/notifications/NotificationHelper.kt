package com.fuku856.povomanager.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fuku856.povomanager.MainActivity
import com.fuku856.povomanager.R
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.ui.common.displayName
import com.fuku856.povomanager.ui.common.toDisplayString
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_EXPIRY = "expiry"
        const val CHANNEL_TOPPING = "topping"
        const val EXTRA_LINE_ID = "lineId"
    }

    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_EXPIRY, "自動解約日", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "180日期限が近づいた回線の通知"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TOPPING, "トッピング有効期限", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "購入済みトッピングの有効期限が近づいた通知"
            }
        )
    }

    fun notifyExpiry(status: LineStatus) {
        val remaining = status.daysRemaining ?: return
        val expiry = status.expiryDate ?: return
        val name = status.line.displayName
        val (title, text) = when {
            remaining < 0 ->
                "【povo】自動解約の可能性: $name" to
                    "期限(${expiry.toDisplayString()})を過ぎています。回線状態を確認してください。"
            remaining == 0L ->
                "【povo】本日が自動解約日です: $name" to
                    "今日中にトッピングを購入しないと自動解約される可能性があります。"
            else ->
                "【povo】解約まであと${remaining}日: $name" to
                    "${expiry.toDisplayString()}までにトッピングの購入が必要です。"
        }
        post(
            channelId = CHANNEL_EXPIRY,
            notificationId = expiryNotificationId(status.line.id),
            lineId = status.line.id,
            title = title,
            text = text,
        )
    }

    fun notifyToppings(line: PovoLine, toppings: List<ToppingPurchase>) {
        if (toppings.isEmpty()) return
        val detail = toppings.joinToString("、") {
            "${it.toppingName}(${it.validityEndDate?.toDisplayString()}まで)"
        }
        post(
            channelId = CHANNEL_TOPPING,
            notificationId = toppingNotificationId(line.id),
            lineId = line.id,
            title = "【povo】トッピング期限が近づいています: ${line.displayName}",
            text = detail,
        )
    }

    private fun post(channelId: String, notificationId: Int, lineId: Long, title: String, text: String) {
        if (!manager.areNotificationsEnabled()) return
        ensureChannels()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LINE_ID, lineId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_sim)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(notificationId, notification) }
    }

    private fun expiryNotificationId(lineId: Long): Int = (lineId * 2).toInt()
    private fun toppingNotificationId(lineId: Long): Int = (lineId * 2 + 1).toInt()
}

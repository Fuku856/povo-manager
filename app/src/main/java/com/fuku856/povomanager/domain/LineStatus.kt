package com.fuku856.povomanager.domain

import com.fuku856.povomanager.data.db.LineWithPurchases
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 回線の期限状態(算出値をまとめたUI/通知用モデル) */
data class LineStatus(
    val line: PovoLine,
    val purchases: List<ToppingPurchase>,
    /** 最終トッピング購入日。履歴がない場合はnull */
    val lastPurchaseDate: LocalDate?,
    /** 自動解約日(最終購入日 + 期限日数)。履歴がない場合はnull */
    val expiryDate: LocalDate?,
    /** 自動解約日までの残日数。期限日当日=0、超過は負値 */
    val daysRemaining: Long?,
    /** 有効期間中のトッピングのうち期限が最も近いもの */
    val activeTopping: ToppingPurchase?,
)

fun LineWithPurchases.toStatus(settings: AppSettings, today: LocalDate): LineStatus {
    val lastPurchase = purchases.maxOfOrNull { it.purchaseDate }
    val expiry = lastPurchase?.plusDays(settings.expiryPeriodDays.toLong())
    return LineStatus(
        line = line,
        purchases = purchases.sortedByDescending { it.purchaseDate },
        lastPurchaseDate = lastPurchase,
        expiryDate = expiry,
        daysRemaining = expiry?.let { ChronoUnit.DAYS.between(today, it) },
        activeTopping = activeTopping(purchases, today),
    )
}

/** 有効期間中(今日が有効期限以前)のトッピングのうち、期限が最も近いものを返す */
fun activeTopping(purchases: List<ToppingPurchase>, today: LocalDate): ToppingPurchase? =
    purchases
        .filter { it.validityEndDate != null && !it.validityEndDate.isBefore(today) }
        .minByOrNull { it.validityEndDate!! }

/** この回線に適用される通知タイミング(回線ごと上書き or 共通設定) */
fun effectiveNotifyDays(line: PovoLine, settings: AppSettings): Set<Int> =
    line.notifyDaysOverride ?: settings.defaultNotifyDays

/** 自動解約日の通知を今日発行すべきか */
fun shouldNotifyExpiry(status: LineStatus, settings: AppSettings): Boolean {
    val remaining = status.daysRemaining ?: return false
    if (remaining < 0) return true // 期限超過は毎日警告
    return effectiveNotifyDays(status.line, settings).any { it.toLong() == remaining }
}

/** 今日通知対象となるトッピング(有効期限が近いもの)を返す */
fun toppingsToNotify(purchases: List<ToppingPurchase>, settings: AppSettings, today: LocalDate): List<ToppingPurchase> =
    purchases.filter { purchase ->
        val end = purchase.validityEndDate ?: return@filter false
        val remaining = ChronoUnit.DAYS.between(today, end)
        remaining >= 0 && remaining.toInt() in settings.toppingExpiryNotifyDays
    }

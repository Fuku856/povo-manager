package com.fuku856.povomanager.domain

import com.fuku856.povomanager.data.db.LineWithPurchases
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
import java.time.LocalDate
import java.time.LocalDateTime
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

/** 自動解約日は日付単位で数えるため、[now] の日付部分を基準にする */
fun LineWithPurchases.toStatus(settings: AppSettings, now: LocalDateTime): LineStatus {
    val today = now.toLocalDate()
    val lastPurchase = purchases.maxOfOrNull { it.purchaseDate }
    val expiry = lastPurchase?.plusDays(settings.expiryPeriodDays.toLong())
    return LineStatus(
        line = line,
        purchases = purchases.sortedWith(
            compareByDescending<ToppingPurchase> { it.purchaseDate }.thenByDescending { it.purchaseTime },
        ),
        lastPurchaseDate = lastPurchase,
        expiryDate = expiry,
        daysRemaining = expiry?.let { ChronoUnit.DAYS.between(today, it) },
        activeTopping = activeTopping(purchases, now),
    )
}

/** 一覧表示用。状態に変換し、残日数の少ない順(購入記録なしは末尾)に並べる */
fun List<LineWithPurchases>.toStatusesByExpiry(settings: AppSettings, now: LocalDateTime): List<LineStatus> =
    map { it.toStatus(settings, now) }.sortedWith(compareBy(nullsLast()) { it.daysRemaining })

/**
 * トッピングが満了する時点(この時刻になった瞬間に使えなくなる)。
 * 満了時刻を持たないもの(日数型・時刻機能より前の記録)は有効期限日の終わり=翌日0:00とする。
 * 期限管理の対象外ならnull。
 */
val ToppingPurchase.validityEndAt: LocalDateTime?
    get() = validityEndDate?.let { date ->
        validityEndTime?.let(date::atTime) ?: date.plusDays(1).atStartOfDay()
    }

/** 有効期間中(まだ満了していない)のトッピングのうち、期限が最も近いものを返す */
fun activeTopping(purchases: List<ToppingPurchase>, now: LocalDateTime): ToppingPurchase? =
    purchases
        .filter { it.validityEndAt?.isAfter(now) == true }
        .minByOrNull { it.validityEndAt!! }

/** この回線に適用される通知タイミング(回線ごと上書き or 共通設定) */
fun effectiveNotifyDays(line: PovoLine, settings: AppSettings): Set<Int> =
    line.notifyDaysOverride ?: settings.defaultNotifyDays

/** 自動解約日の通知を今日発行すべきか */
fun shouldNotifyExpiry(status: LineStatus, settings: AppSettings): Boolean {
    val remaining = status.daysRemaining ?: return false
    if (remaining < 0) return true // 期限超過は毎日警告
    return effectiveNotifyDays(status.line, settings).any { it.toLong() == remaining }
}

/**
 * 今日通知対象となるトッピング(有効期限が「〜日前」の設定に一致するもの)を返す。
 * 日数は日付単位で数えるが、[now] の時点で既に満了したもの(朝に切れた24時間型など)は除く。
 */
fun toppingsToNotify(purchases: List<ToppingPurchase>, settings: AppSettings, now: LocalDateTime): List<ToppingPurchase> =
    purchases.filter { purchase ->
        val end = purchase.validityEndDate ?: return@filter false
        if (purchase.validityEndAt?.isAfter(now) != true) return@filter false
        val remaining = ChronoUnit.DAYS.between(now.toLocalDate(), end)
        remaining >= 0 && remaining.toInt() in settings.toppingExpiryNotifyDays
    }

/** 「〜時間前」通知の発火予定1件 */
data class ToppingHourAlert(
    val line: PovoLine,
    val purchase: ToppingPurchase,
    /** トッピングが満了する時点 */
    val endAt: LocalDateTime,
    /** 通知する時刻(満了の hoursBefore 時間前) */
    val triggerAt: LocalDateTime,
)

/** 期限のある全トッピング × 設定された「〜時間前」の組み合わせで発火予定を列挙する */
fun toppingHourAlerts(lines: List<LineWithPurchases>, settings: AppSettings): List<ToppingHourAlert> =
    lines.flatMap { item ->
        item.purchases.flatMap { purchase ->
            val endAt = purchase.validityEndAt ?: return@flatMap emptyList()
            settings.toppingExpiryNotifyHours.map { hours ->
                ToppingHourAlert(item.line, purchase, endAt, endAt.minusHours(hours.toLong()))
            }
        }
    }

/** [now] より後で最も早い「〜時間前」通知の時刻。予定がなければnull */
fun nextToppingAlertAt(lines: List<LineWithPurchases>, settings: AppSettings, now: LocalDateTime): LocalDateTime? =
    toppingHourAlerts(lines, settings)
        .filter { it.triggerAt.isAfter(now) }
        .minOfOrNull { it.triggerAt }

/**
 * 発火時刻が (from, to] に入り、[to] の時点でまだ満了していないトッピングを回線ごとに返す。
 * 同じトッピングの複数の発火(例: 3時間前と1時間前)が区間に入っても1件にまとめる。
 */
fun toppingAlertsDue(
    lines: List<LineWithPurchases>,
    settings: AppSettings,
    from: LocalDateTime,
    to: LocalDateTime,
): Map<PovoLine, List<ToppingPurchase>> =
    toppingHourAlerts(lines, settings)
        .filter { it.triggerAt.isAfter(from) && !it.triggerAt.isAfter(to) && it.endAt.isAfter(to) }
        .groupBy({ it.line }, { it.purchase })
        .mapValues { (_, purchases) -> purchases.distinct() }

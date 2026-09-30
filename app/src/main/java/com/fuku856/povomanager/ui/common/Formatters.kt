package com.fuku856.povomanager.ui.common

import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDate.toDisplayString(): String = format(DATE_FORMAT)

fun LocalTime.toDisplayString(): String = format(TIME_FORMAT)

fun LocalDateTime.toDisplayString(): String = "${toLocalDate().toDisplayString()} ${toLocalTime().toDisplayString()}"

/** 日付と、あれば時刻を表示する(時刻機能より前の記録や日数型の期限は日付のみ) */
fun formatDateWithTime(date: LocalDate, time: LocalTime?): String =
    time?.let { date.atTime(it).toDisplayString() } ?: date.toDisplayString()

/** 購入日時の表示(時刻が未記録なら日付のみ) */
val ToppingPurchase.purchaseDisplayString: String
    get() = formatDateWithTime(purchaseDate, purchaseTime)

/** 有効期限の表示(満了時刻がない日数型は日付のみ)。期限管理なしはnull */
val ToppingPurchase.validityEndDisplayString: String?
    get() = validityEndDate?.let { formatDateWithTime(it, validityEndTime) }

/** 残り時間を「あと○日○時間」「あと○時間○分」「あと○分」の形で表す。0以下は「期限切れ」 */
fun formatRemaining(remaining: Duration): String {
    if (remaining.isNegative || remaining.isZero) return "期限切れ"
    // 表示は分単位で切り上げる(残り30秒を「あと0分」と出さない)
    val totalMinutes = (remaining.seconds + 59) / 60
    val days = totalMinutes / (24 * 60)
    val hours = totalMinutes / 60 % 24
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> if (hours > 0) "あと${days}日${hours}時間" else "あと${days}日"
        hours > 0 -> if (minutes > 0) "あと${hours}時間${minutes}分" else "あと${hours}時間"
        else -> "あと${minutes}分"
    }
}

/**
 * 携帯/IP電話(050・070・080・090始まり、11桁)は 3-4-4、
 * それ以外(固定電話、10桁)は 2-4-4 のグループ区切りで整形する。
 * 入力欄のハイフン表示([PhoneNumberVisualTransformation])と区切り規則を共有する。
 */
private val MOBILE_PREFIXES = listOf("050", "070", "080", "090")

fun phoneGroupSizes(digits: String): List<Int> =
    if (MOBILE_PREFIXES.any { digits.startsWith(it) }) listOf(3, 4, 4) else listOf(2, 4, 4)

/**
 * ハイフンを挿入すべき桁位置(その桁の直前に `-` が入る)を返す。
 * 区切り位置より後ろに数字が無い場合(末尾)は表示しないため除外する。
 */
fun phoneHyphenBoundaries(digits: String): List<Int> {
    val groups = phoneGroupSizes(digits)
    val result = mutableListOf<Int>()
    var acc = 0
    for (i in 0 until groups.size - 1) {
        acc += groups[i]
        if (acc < digits.length) result.add(acc)
    }
    return result
}

/** 11桁なら 080-1234-5678 形式、10桁なら 03-1234-5678 形式に整形 */
fun formatPhoneNumber(raw: String): String {
    if (raw.length !in 10..11) return raw
    val boundaries = phoneHyphenBoundaries(raw).toSet()
    return buildString {
        for (i in raw.indices) {
            if (i in boundaries) append('-')
            append(raw[i])
        }
    }
}

val PovoLine.displayName: String
    get() = name?.takeIf { it.isNotBlank() } ?: formatPhoneNumber(phoneNumber)

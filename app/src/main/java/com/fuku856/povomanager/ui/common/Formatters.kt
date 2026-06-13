package com.fuku856.povomanager.ui.common

import com.fuku856.povomanager.data.db.PovoLine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

fun LocalDate.toDisplayString(): String = format(DATE_FORMAT)

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

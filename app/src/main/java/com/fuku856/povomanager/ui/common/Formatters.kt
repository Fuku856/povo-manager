package com.fuku856.povomanager.ui.common

import com.fuku856.povomanager.data.db.PovoLine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

fun LocalDate.toDisplayString(): String = format(DATE_FORMAT)

/** 11桁なら 080-1234-5678 形式、10桁なら 03-1234-5678 形式に整形 */
fun formatPhoneNumber(raw: String): String = when (raw.length) {
    11 -> "${raw.substring(0, 3)}-${raw.substring(3, 7)}-${raw.substring(7)}"
    10 -> "${raw.substring(0, 2)}-${raw.substring(2, 6)}-${raw.substring(6)}"
    else -> raw
}

val PovoLine.displayName: String
    get() = name?.takeIf { it.isNotBlank() } ?: formatPhoneNumber(phoneNumber)

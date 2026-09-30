package com.fuku856.povomanager.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * povo 2.0 の主要トッピングのプリセット。
 * validityDays・validityHours がともにnullのものは自動更新型などで有効期限管理の対象外。
 * ラインナップや価格は変わりうるため、自由入力も常に可能にする。
 */
data class ToppingPreset(
    val name: String,
    /**
     * 日数型の有効日数。povoの規定では購入時刻から24時間ごとに1日と数え、
     * 期間満了日(購入日+日数)の23:59:59まで使える。
     */
    val validityDays: Int? = null,
    /** 時間型(データ使い放題)の有効時間。購入時刻からこの時間ちょうどで満了する */
    val validityHours: Int? = null,
)

/** 有効期限。time が null なら date の終わり(23:59:59)まで有効 */
data class ValidityEnd(val date: LocalDate, val time: LocalTime?)

/** 購入日時からこのプリセットの有効期限を求める。期限管理の対象外ならnull */
fun ToppingPreset.validityEnd(purchasedAt: LocalDateTime): ValidityEnd? {
    validityHours?.let { hours ->
        val end = purchasedAt.plusHours(hours.toLong())
        return ValidityEnd(end.toLocalDate(), end.toLocalTime())
    }
    validityDays?.let { days ->
        return ValidityEnd(purchasedAt.toLocalDate().plusDays(days.toLong()), null)
    }
    return null
}

val TOPPING_PRESETS = listOf(
    ToppingPreset("データ追加1GB(7日間)", validityDays = 7),
    ToppingPreset("データ追加3GB(30日間)", validityDays = 30),
    ToppingPreset("データ追加20GB(30日間)", validityDays = 30),
    ToppingPreset("データ追加60GB(90日間)", validityDays = 90),
    ToppingPreset("データ追加150GB(180日間)", validityDays = 180),
    ToppingPreset("データ使い放題(24時間)", validityHours = 24),
    ToppingPreset("5分以内通話かけ放題"),
    ToppingPreset("通話かけ放題"),
)

package com.fuku856.povomanager.domain

/**
 * povo 2.0 の主要トッピングのプリセット。
 * validityDays がnullのものは自動更新型などで有効期限管理の対象外。
 * ラインナップや価格は変わりうるため、自由入力も常に可能にする。
 */
data class ToppingPreset(
    val name: String,
    /** 購入日を1日目とした有効日数。null=期限管理なし */
    val validityDays: Int?,
)

val TOPPING_PRESETS = listOf(
    ToppingPreset("データ追加1GB(7日間)", 7),
    ToppingPreset("データ追加3GB(30日間)", 30),
    ToppingPreset("データ追加20GB(30日間)", 30),
    ToppingPreset("データ追加60GB(90日間)", 90),
    ToppingPreset("データ追加150GB(180日間)", 180),
    ToppingPreset("データ使い放題(24時間)", 1),
    ToppingPreset("5分以内通話かけ放題", null),
    ToppingPreset("通話かけ放題", null),
)

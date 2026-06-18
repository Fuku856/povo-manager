package com.fuku856.povomanager.domain

/**
 * 回線のSIM種別。DBには enum 名([name])で保存する。
 * この機能より前に登録された回線は null(未設定)。
 */
enum class SimType(val label: String) {
    PHYSICAL("物理SIM"),
    ESIM("eSIM"),
}

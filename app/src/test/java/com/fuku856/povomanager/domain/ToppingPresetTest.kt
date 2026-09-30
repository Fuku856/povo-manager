package com.fuku856.povomanager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ToppingPresetTest {

    private fun preset(name: String) = TOPPING_PRESETS.first { it.name == name }

    @Test
    fun `24時間使い放題は購入時刻からちょうど24時間で満了する`() {
        // povo公式の例: 15時に購入 → 翌日15時で満了(14:59まで使える)
        val end = preset("データ使い放題(24時間)").validityEnd(LocalDateTime.of(2026, 9, 30, 15, 0))
        assertEquals(ValidityEnd(LocalDate.of(2026, 10, 1), LocalTime.of(15, 0)), end)
    }

    @Test
    fun `日数型は購入日から日数後の日の終わりまで有効`() {
        // 購入時刻から7×24時間後の日(期間満了日)の23:59:59まで → 購入日+7日、時刻なし
        val end = preset("データ追加1GB(7日間)").validityEnd(LocalDateTime.of(2026, 9, 30, 23, 30))
        assertEquals(ValidityEnd(LocalDate.of(2026, 10, 7), null), end)
    }

    @Test
    fun `30日型は月をまたいでも購入日から30日後`() {
        val end = preset("データ追加3GB(30日間)").validityEnd(LocalDateTime.of(2026, 1, 31, 8, 0))
        assertEquals(ValidityEnd(LocalDate.of(2026, 3, 2), null), end)
    }

    @Test
    fun `期限管理のないトッピングはnull`() {
        assertNull(preset("通話かけ放題").validityEnd(LocalDateTime.of(2026, 9, 30, 15, 0)))
    }
}

package com.fuku856.povomanager.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class FormatRemainingTest {

    @Test
    fun `1日未満は時間と分で表す`() {
        assertEquals("あと5時間23分", formatRemaining(Duration.ofHours(5).plusMinutes(23)))
        assertEquals("あと1時間", formatRemaining(Duration.ofHours(1)))
        assertEquals("あと45分", formatRemaining(Duration.ofMinutes(45)))
    }

    @Test
    fun `1日以上は日と時間で表す`() {
        assertEquals("あと1日2時間", formatRemaining(Duration.ofHours(26).plusMinutes(10)))
        assertEquals("あと3日", formatRemaining(Duration.ofDays(3)))
    }

    @Test
    fun `秒の端数は分に切り上げる`() {
        assertEquals("あと1分", formatRemaining(Duration.ofSeconds(30)))
        assertEquals("あと1時間", formatRemaining(Duration.ofMinutes(59).plusSeconds(1)))
    }

    @Test
    fun `0以下は期限切れ`() {
        assertEquals("期限切れ", formatRemaining(Duration.ZERO))
        assertEquals("期限切れ", formatRemaining(Duration.ofMinutes(-5)))
    }
}

package com.fuku856.povomanager.domain

import com.fuku856.povomanager.data.db.LineWithPurchases
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LineStatusTest {

    private val today: LocalDate = LocalDate.of(2026, 6, 11)
    private val settings = AppSettings()
    private val line = PovoLine(id = 1, phoneNumber = "08012345678")

    private fun purchase(
        date: LocalDate,
        validityEnd: LocalDate? = null,
        id: Long = 0,
    ) = ToppingPurchase(id = id, lineId = 1, purchaseDate = date, toppingName = "テスト", validityEndDate = validityEnd)

    @Test
    fun `購入履歴がない場合は期限なし`() {
        val status = LineWithPurchases(line, emptyList()).toStatus(settings, today)
        assertNull(status.lastPurchaseDate)
        assertNull(status.expiryDate)
        assertNull(status.daysRemaining)
    }

    @Test
    fun `最終購入日は履歴の最大値`() {
        val status = LineWithPurchases(
            line,
            listOf(
                purchase(LocalDate.of(2026, 1, 10)),
                purchase(LocalDate.of(2026, 3, 5)),
                purchase(LocalDate.of(2025, 12, 1)),
            ),
        ).toStatus(settings, today)
        assertEquals(LocalDate.of(2026, 3, 5), status.lastPurchaseDate)
    }

    @Test
    fun `解約期限は最終購入日の180日後`() {
        val status = LineWithPurchases(line, listOf(purchase(LocalDate.of(2026, 1, 1))))
            .toStatus(settings, today)
        assertEquals(LocalDate.of(2026, 6, 30), status.expiryDate)
        assertEquals(19L, status.daysRemaining)
    }

    @Test
    fun `期限日数の設定変更が反映される`() {
        val status = LineWithPurchases(line, listOf(purchase(LocalDate.of(2026, 1, 1))))
            .toStatus(settings.copy(expiryPeriodDays = 90), today)
        assertEquals(LocalDate.of(2026, 4, 1), status.expiryDate)
    }

    @Test
    fun `期限当日の残日数は0`() {
        val status = LineWithPurchases(line, listOf(purchase(today.minusDays(180))))
            .toStatus(settings, today)
        assertEquals(0L, status.daysRemaining)
        assertTrue(shouldNotifyExpiry(status, settings.copy(defaultNotifyDays = setOf(0))))
    }

    @Test
    fun `期限超過は常に通知対象`() {
        val status = LineWithPurchases(line, listOf(purchase(today.minusDays(200))))
            .toStatus(settings, today)
        assertEquals(-20L, status.daysRemaining)
        assertTrue(shouldNotifyExpiry(status, settings))
    }

    @Test
    fun `共通設定の通知日数に一致した日のみ通知`() {
        val statusAt7 = LineWithPurchases(line, listOf(purchase(today.minusDays(173))))
            .toStatus(settings, today)
        assertEquals(7L, statusAt7.daysRemaining)
        assertTrue(shouldNotifyExpiry(statusAt7, settings))

        val statusAt8 = LineWithPurchases(line, listOf(purchase(today.minusDays(172))))
            .toStatus(settings, today)
        assertEquals(8L, statusAt8.daysRemaining)
        assertFalse(shouldNotifyExpiry(statusAt8, settings))
    }

    @Test
    fun `回線ごとの上書き設定が優先される`() {
        val overrideLine = line.copy(notifyDaysOverride = setOf(10))
        val status = LineWithPurchases(overrideLine, listOf(purchase(today.minusDays(170))))
            .toStatus(settings, today)
        assertEquals(10L, status.daysRemaining)
        assertTrue(shouldNotifyExpiry(status, settings))
        // 共通設定では10日前は通知対象外
        val defaultStatus = LineWithPurchases(line, listOf(purchase(today.minusDays(170))))
            .toStatus(settings, today)
        assertFalse(shouldNotifyExpiry(defaultStatus, settings))
    }

    @Test
    fun `上書きが空集合なら通知しない`() {
        val mutedLine = line.copy(notifyDaysOverride = emptySet())
        val status = LineWithPurchases(mutedLine, listOf(purchase(today.minusDays(173))))
            .toStatus(settings, today)
        assertFalse(shouldNotifyExpiry(status, settings))
    }

    @Test
    fun `有効中トッピングは期限が最も近いものを返す`() {
        val purchases = listOf(
            purchase(today.minusDays(5), validityEnd = today.plusDays(25), id = 1),
            purchase(today.minusDays(3), validityEnd = today.plusDays(4), id = 2),
            purchase(today.minusDays(40), validityEnd = today.minusDays(10), id = 3), // 期限切れ
            purchase(today.minusDays(1), validityEnd = null, id = 4), // 期限管理なし
        )
        val active = activeTopping(purchases, today)
        assertEquals(2L, active?.id)
    }

    @Test
    fun `トッピング期限通知は設定日数に一致したもののみ`() {
        val purchases = listOf(
            purchase(today.minusDays(6), validityEnd = today.plusDays(1), id = 1), // 残1日 → 通知
            purchase(today.minusDays(2), validityEnd = today.plusDays(5), id = 2), // 残5日 → 対象外
            purchase(today.minusDays(40), validityEnd = today.minusDays(1), id = 3), // 期限切れ → 対象外
        )
        val toNotify = toppingsToNotify(purchases, settings, today)
        assertEquals(listOf(1L), toNotify.map { it.id })
    }
}

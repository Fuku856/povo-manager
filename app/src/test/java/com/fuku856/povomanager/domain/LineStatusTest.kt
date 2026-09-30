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
import java.time.LocalDateTime
import java.time.LocalTime

class LineStatusTest {

    private val today: LocalDate = LocalDate.of(2026, 6, 11)
    private val now: LocalDateTime = today.atTime(12, 0)
    private val settings = AppSettings()
    private val line = PovoLine(id = 1, phoneNumber = "08012345678")

    private fun purchase(
        date: LocalDate,
        validityEnd: LocalDate? = null,
        id: Long = 0,
        validityEndTime: LocalTime? = null,
    ) = ToppingPurchase(
        id = id,
        lineId = 1,
        purchaseDate = date,
        toppingName = "テスト",
        validityEndDate = validityEnd,
        validityEndTime = validityEndTime,
    )

    @Test
    fun `購入履歴がない場合は期限なし`() {
        val status = LineWithPurchases(line, emptyList()).toStatus(settings, now)
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
        ).toStatus(settings, now)
        assertEquals(LocalDate.of(2026, 3, 5), status.lastPurchaseDate)
    }

    @Test
    fun `自動解約日は最終購入日の180日後`() {
        val status = LineWithPurchases(line, listOf(purchase(LocalDate.of(2026, 1, 1))))
            .toStatus(settings, now)
        assertEquals(LocalDate.of(2026, 6, 30), status.expiryDate)
        assertEquals(19L, status.daysRemaining)
    }

    @Test
    fun `期限日数の設定変更が反映される`() {
        val status = LineWithPurchases(line, listOf(purchase(LocalDate.of(2026, 1, 1))))
            .toStatus(settings.copy(expiryPeriodDays = 90), now)
        assertEquals(LocalDate.of(2026, 4, 1), status.expiryDate)
    }

    @Test
    fun `期限当日の残日数は0`() {
        val status = LineWithPurchases(line, listOf(purchase(today.minusDays(180))))
            .toStatus(settings, now)
        assertEquals(0L, status.daysRemaining)
        assertTrue(shouldNotifyExpiry(status, settings.copy(defaultNotifyDays = setOf(0))))
    }

    @Test
    fun `期限当日はデフォルト設定で通知される`() {
        val status = LineWithPurchases(line, listOf(purchase(today.minusDays(180))))
            .toStatus(settings, now)
        assertEquals(0L, status.daysRemaining)
        // DEFAULT_NOTIFY_DAYS に 0 を含むため、自動解約の当日に通知される
        assertTrue(shouldNotifyExpiry(status, settings))
    }

    @Test
    fun `期限超過は常に通知対象`() {
        val status = LineWithPurchases(line, listOf(purchase(today.minusDays(200))))
            .toStatus(settings, now)
        assertEquals(-20L, status.daysRemaining)
        assertTrue(shouldNotifyExpiry(status, settings))
    }

    @Test
    fun `共通設定の通知日数に一致した日のみ通知`() {
        val statusAt7 = LineWithPurchases(line, listOf(purchase(today.minusDays(173))))
            .toStatus(settings, now)
        assertEquals(7L, statusAt7.daysRemaining)
        assertTrue(shouldNotifyExpiry(statusAt7, settings))

        val statusAt8 = LineWithPurchases(line, listOf(purchase(today.minusDays(172))))
            .toStatus(settings, now)
        assertEquals(8L, statusAt8.daysRemaining)
        assertFalse(shouldNotifyExpiry(statusAt8, settings))
    }

    @Test
    fun `回線ごとの上書き設定が優先される`() {
        val overrideLine = line.copy(notifyDaysOverride = setOf(10))
        val status = LineWithPurchases(overrideLine, listOf(purchase(today.minusDays(170))))
            .toStatus(settings, now)
        assertEquals(10L, status.daysRemaining)
        assertTrue(shouldNotifyExpiry(status, settings))
        // 共通設定では10日前は通知対象外
        val defaultStatus = LineWithPurchases(line, listOf(purchase(today.minusDays(170))))
            .toStatus(settings, now)
        assertFalse(shouldNotifyExpiry(defaultStatus, settings))
    }

    @Test
    fun `上書きが空集合なら通知しない`() {
        val mutedLine = line.copy(notifyDaysOverride = emptySet())
        val status = LineWithPurchases(mutedLine, listOf(purchase(today.minusDays(173))))
            .toStatus(settings, now)
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
        val active = activeTopping(purchases, now)
        assertEquals(2L, active?.id)
    }

    @Test
    fun `トッピング期限通知は設定日数に一致したもののみ`() {
        val purchases = listOf(
            purchase(today.minusDays(6), validityEnd = today.plusDays(1), id = 1), // 残1日 → 通知
            purchase(today.minusDays(2), validityEnd = today.plusDays(5), id = 2), // 残5日 → 対象外
            purchase(today.minusDays(40), validityEnd = today.minusDays(1), id = 3), // 期限切れ → 対象外
        )
        val toNotify = toppingsToNotify(purchases, settings, now)
        assertEquals(listOf(1L), toNotify.map { it.id })
    }

    @Test
    fun `時間型トッピングは満了時刻を過ぎたら有効中から外れる`() {
        // 昨日13:00に購入した24時間型 → 今日13:00に満了
        val dayPass = purchase(today.minusDays(1), validityEnd = today, id = 1, validityEndTime = LocalTime.of(13, 0))
        assertEquals(1L, activeTopping(listOf(dayPass), today.atTime(12, 59))?.id)
        assertNull(activeTopping(listOf(dayPass), today.atTime(13, 0)))
    }

    @Test
    fun `時刻のない有効期限は期限日の終わりまで有効`() {
        val weekly = purchase(today.minusDays(7), validityEnd = today, id = 1)
        assertEquals(1L, activeTopping(listOf(weekly), today.atTime(23, 59))?.id)
        assertNull(activeTopping(listOf(weekly), today.plusDays(1).atStartOfDay()))
    }

    @Test
    fun `有効中トッピングは同じ日なら満了時刻の早いものを返す`() {
        val purchases = listOf(
            purchase(today.minusDays(7), validityEnd = today, id = 1), // 今日の終わりまで
            purchase(today.minusDays(1), validityEnd = today, id = 2, validityEndTime = LocalTime.of(18, 0)),
        )
        assertEquals(2L, activeTopping(purchases, now)?.id)
    }

    @Test
    fun `日数前のトッピング通知は満了済みの時間型を除外する`() {
        val oneDaySettings = settings.copy(toppingExpiryNotifyDays = setOf(0))
        // 今日9:00に満了した24時間型は、通知時刻(12:00)には既に切れているので「当日」通知しない
        val expired = purchase(today.minusDays(1), validityEnd = today, id = 1, validityEndTime = LocalTime.of(9, 0))
        // 今日15:00に満了する24時間型は「当日」として通知する
        val active = purchase(today.minusDays(1), validityEnd = today, id = 2, validityEndTime = LocalTime.of(15, 0))
        val toNotify = toppingsToNotify(listOf(expired, active), oneDaySettings, now)
        assertEquals(listOf(2L), toNotify.map { it.id })
    }

    @Test
    fun `購入履歴は同じ日なら時刻の新しい順に並ぶ`() {
        val purchases = listOf(
            purchase(today, id = 1).copy(purchaseTime = LocalTime.of(8, 0)),
            purchase(today, id = 2).copy(purchaseTime = LocalTime.of(20, 0)),
            purchase(today.minusDays(1), id = 3).copy(purchaseTime = LocalTime.of(23, 0)),
        )
        val status = LineWithPurchases(line, purchases).toStatus(settings, now)
        assertEquals(listOf(2L, 1L, 3L), status.purchases.map { it.id })
    }
}

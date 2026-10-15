package com.fuku856.povomanager.domain

import com.fuku856.povomanager.data.db.LineWithPurchases
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** トッピング期限の「〜時間前」通知の発火時刻計算 */
class ToppingAlertTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 30)
    private val line = PovoLine(id = 1, phoneNumber = "08012345678")
    private val settings = AppSettings(toppingExpiryNotifyHours = setOf(3, 1))

    /** 今日15:00に満了する24時間型 */
    private val dayPass = ToppingPurchase(
        id = 10,
        lineId = 1,
        purchaseDate = today.minusDays(1),
        purchaseTime = LocalTime.of(15, 0),
        toppingName = "データ使い放題(24時間)",
        validityEndDate = today,
        validityEndTime = LocalTime.of(15, 0),
    )

    /** 今日の終わり(翌日0:00)に満了する日数型 */
    private val weekly = ToppingPurchase(
        id = 20,
        lineId = 1,
        purchaseDate = today.minusDays(7),
        toppingName = "データ追加1GB(7日間)",
        validityEndDate = today,
    )

    private fun lines(vararg purchases: ToppingPurchase) = listOf(LineWithPurchases(line, purchases.toList()))

    @Test
    fun `次回の発火時刻は現在より後で最も早いもの`() {
        val lines = lines(dayPass, weekly)
        // 候補: 24時間型 12:00・14:00、日数型 21:00・23:00
        assertEquals(today.atTime(12, 0), nextToppingAlertAt(lines, settings, today.atTime(9, 0)))
        // 発火時刻ちょうどは「後」ではないので次の候補
        assertEquals(today.atTime(14, 0), nextToppingAlertAt(lines, settings, today.atTime(12, 0)))
        assertEquals(today.atTime(21, 0), nextToppingAlertAt(lines, settings, today.atTime(14, 0)))
    }

    @Test
    fun `日数型は期限日の24時を基準に数える`() {
        assertEquals(
            today.atTime(23, 0),
            nextToppingAlertAt(lines(weekly), settings, today.atTime(21, 0)),
        )
    }

    @Test
    fun `予定がなければnull`() {
        val noLimit = weekly.copy(validityEndDate = null)
        assertNull(nextToppingAlertAt(lines(noLimit), settings, today.atTime(9, 0)))
        assertNull(nextToppingAlertAt(lines(dayPass), settings.copy(toppingExpiryNotifyHours = emptySet()), today.atTime(9, 0)))
        // すべての発火時刻が過ぎている
        assertNull(nextToppingAlertAt(lines(dayPass), settings, today.atTime(14, 0)))
    }

    @Test
    fun `通知対象は区間の開始を含まず終了を含む`() {
        val lines = lines(dayPass)
        val from = today.atTime(12, 0)
        // 12:00 の発火は開始時刻ちょうどなので対象外(前回の確認で通知済み)
        assertTrue(toppingAlertsDue(lines, settings, from, today.atTime(13, 59)).isEmpty())
        // 14:00 の発火は終了時刻ちょうどなので対象
        assertEquals(
            mapOf(line to listOf(dayPass)),
            toppingAlertsDue(lines, settings, from, today.atTime(14, 0)),
        )
    }

    @Test
    fun `同じトッピングの複数の発火は1件にまとめる`() {
        // 端末の電源が切れていて 3時間前・1時間前 をまとめて確認したケース
        val due = toppingAlertsDue(lines(dayPass), settings, today.atTime(11, 0), today.atTime(14, 30))
        assertEquals(listOf(dayPass), due[line])
    }

    @Test
    fun `確認時点で満了済みのトッピングは通知しない`() {
        val due = toppingAlertsDue(lines(dayPass), settings, today.atTime(11, 0), today.atTime(15, 0))
        assertTrue(due.isEmpty())
    }

    @Test
    fun `時間型と日数型をひとつの回線にまとめる`() {
        val due = toppingAlertsDue(
            lines(dayPass, weekly),
            settings.copy(toppingExpiryNotifyHours = setOf(12, 3)),
            from = today.atTime(10, 0),
            to = today.atTime(12, 0),
        )
        // 24時間型の3時間前(12:00)と日数型の12時間前(12:00)
        assertEquals(setOf(dayPass, weekly), due[line]?.toSet())
    }

    @Test
    fun `hourBefore の発火時刻は満了時刻から逆算する`() {
        val alerts = toppingHourAlerts(lines(dayPass), settings.copy(toppingExpiryNotifyHours = setOf(12)))
        assertEquals(listOf(LocalDateTime.of(2026, 9, 30, 3, 0)), alerts.map { it.triggerAt })
    }
}

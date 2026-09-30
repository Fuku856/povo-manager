package com.fuku856.povomanager.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fuku856.povomanager.domain.SimType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/** [LineDao] のインポート・アーカイブ更新の検証(Robolectric + インメモリ Room)。 */
@RunWith(AndroidJUnit4::class)
class LineDaoTest {
    private lateinit var db: PovoDatabase
    private lateinit var dao: LineDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            PovoDatabase::class.java,
        ).build()
        dao = db.lineDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mergeImport_keepsExistingArchiveAndSimType_forLegacyBackupLine() = runBlocking {
        val id = dao.insertLine(
            PovoLine(phoneNumber = "09012345678", simType = SimType.ESIM, isArchived = true),
        )

        // 旧形式のバックアップ: simType なし、isArchived なし(既定値 false で読み込まれる)
        dao.mergeImport(
            lines = listOf(PovoLine(phoneNumber = "09012345678", name = "取り込み")),
            purchasesByLineIndex = emptyMap(),
            keepArchivedIndices = setOf(0),
        )

        val merged = dao.getLine(id)!!
        assertEquals("取り込み", merged.name)
        assertEquals(SimType.ESIM, merged.simType)
        assertTrue(merged.isArchived)
    }

    @Test
    fun mergeImport_appliesArchiveState_whenBackupHasIt() = runBlocking {
        val id = dao.insertLine(PovoLine(phoneNumber = "09012345678", isArchived = true))

        dao.mergeImport(
            lines = listOf(PovoLine(phoneNumber = "09012345678", isArchived = false)),
            purchasesByLineIndex = emptyMap(),
        )

        assertFalse(dao.getLine(id)!!.isArchived)
    }

    @Test
    fun mergeImport_treatsPurchasesAtDifferentTimesAsDistinct() = runBlocking {
        val id = dao.insertLine(PovoLine(phoneNumber = "09012345678"))
        val date = LocalDate.of(2026, 9, 30)
        // 日数型は満了時刻を持たないため、同じ日に2回買うと購入時刻だけが異なる
        val existing = ToppingPurchase(
            lineId = id,
            purchaseDate = date,
            purchaseTime = LocalTime.of(9, 0),
            toppingName = "データ追加1GB(7日間)",
            validityEndDate = date.plusDays(7),
        )
        dao.insertPurchase(existing)

        // 時刻が同じものは重複として追加せず、違うものは別の購入として追加する
        val sameTime = existing.copy(lineId = 0)
        val otherTime = existing.copy(lineId = 0, purchaseTime = LocalTime.of(21, 0))
        dao.mergeImport(
            lines = listOf(PovoLine(phoneNumber = "09012345678")),
            purchasesByLineIndex = mapOf(0 to listOf(sameTime, otherTime)),
        )

        val purchases = dao.getPurchasesForLine(id)
        assertEquals(
            setOf(LocalTime.of(9, 0), LocalTime.of(21, 0)),
            purchases.map { it.purchaseTime }.toSet(),
        )
        assertEquals(2, purchases.size)
    }

    @Test
    fun updateArchived_changesOnlyArchiveColumn() = runBlocking {
        val id = dao.insertLine(
            PovoLine(phoneNumber = "09012345678", name = "メイン", simType = SimType.PHYSICAL, sortOrder = 3),
        )

        assertEquals(1, dao.updateArchived(id, true))

        val line = dao.getLine(id)!!
        assertTrue(line.isArchived)
        assertEquals("メイン", line.name)
        assertEquals(SimType.PHYSICAL, line.simType)
        assertEquals(3, line.sortOrder)
    }

    @Test
    fun updateArchived_returnsZero_whenAlreadyInStateOrMissing() = runBlocking {
        val id = dao.insertLine(PovoLine(phoneNumber = "09012345678", isArchived = true))

        // 連打などで二重に呼ばれたケース。状態は変わらないので 0 を返す
        assertEquals(0, dao.updateArchived(id, true))
        assertTrue(dao.getLine(id)!!.isArchived)
        // 削除済みの回線
        assertEquals(0, dao.updateArchived(id + 100, false))
    }
}

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
    fun updateArchived_changesOnlyArchiveColumn() = runBlocking {
        val id = dao.insertLine(
            PovoLine(phoneNumber = "09012345678", name = "メイン", simType = SimType.PHYSICAL, sortOrder = 3),
        )

        dao.updateArchived(id, true)

        val line = dao.getLine(id)!!
        assertTrue(line.isArchived)
        assertEquals("メイン", line.name)
        assertEquals(SimType.PHYSICAL, line.simType)
        assertEquals(3, line.sortOrder)
    }
}

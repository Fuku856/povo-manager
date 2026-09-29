package com.fuku856.povomanager.ui.common

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.db.LineDao
import com.fuku856.povomanager.data.db.PovoDatabase
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** [UndoController] の取り消し処理の検証(Robolectric + インメモリ Room)。 */
@RunWith(AndroidJUnit4::class)
class UndoControllerTest {
    private lateinit var db: PovoDatabase
    private lateinit var dao: LineDao
    private lateinit var repository: LineRepository
    private lateinit var controller: UndoController

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PovoDatabase::class.java).build()
        dao = db.lineDao()
        repository = LineRepository(dao, WidgetUpdater(context))
        controller = UndoController(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun undoArchiveChanged_worksAfterOriginScopeIsCancelled() = runBlocking {
        val lineId = dao.insertLine(PovoLine(phoneNumber = "09012345678", isArchived = true))

        // アーカイブ済み画面で解除し、トースト表示中に戻って ViewModel が破棄された想定
        val viewModelScope = CoroutineScope(Job())
        viewModelScope.launch { repository.setArchived(lineId, false) }.join()
        viewModelScope.cancel()

        controller.undo(UndoableAction.ArchiveChanged(lineId, archived = false)).join()

        assertTrue(dao.getLine(lineId)!!.isArchived)
    }

    @Test
    fun undoArchiveChanged_unarchivesArchivedLine() = runBlocking {
        val lineId = dao.insertLine(PovoLine(phoneNumber = "09012345678", isArchived = true))

        controller.undo(UndoableAction.ArchiveChanged(lineId, archived = true)).join()

        assertFalse(dao.getLine(lineId)!!.isArchived)
    }

    @Test
    fun undoPurchaseAdded_deletesPurchase() = runBlocking {
        val lineId = dao.insertLine(PovoLine(phoneNumber = "09012345678"))
        val purchase = purchase(lineId)
        val id = dao.insertPurchase(purchase)

        controller.undo(UndoableAction.PurchaseAdded(purchase.copy(id = id))).join()

        assertTrue(dao.getPurchasesForLine(lineId).isEmpty())
    }

    @Test
    fun undoPurchaseDeleted_restoresPurchase() = runBlocking {
        val lineId = dao.insertLine(PovoLine(phoneNumber = "09012345678"))
        val purchase = purchase(lineId)
        val deleted = purchase.copy(id = dao.insertPurchase(purchase))
        dao.deletePurchase(deleted)

        controller.undo(UndoableAction.PurchaseDeleted(deleted)).join()

        val restored = dao.getPurchasesForLine(lineId).single()
        assertEquals(purchase.purchaseDate, restored.purchaseDate)
        assertEquals(purchase.toppingName, restored.toppingName)
    }

    @Test
    fun undoPurchaseDeleted_doesNothingWhenLineWasDeleted() = runBlocking {
        // 履歴を削除したあと、トーストが残っている間に回線ごと削除された場合
        val line = PovoLine(phoneNumber = "09012345678")
        val lineId = dao.insertLine(line)
        val purchase = purchase(lineId)
        val deleted = purchase.copy(id = dao.insertPurchase(purchase))
        dao.deleteLine(line.copy(id = lineId))

        val job = controller.undo(UndoableAction.PurchaseDeleted(deleted))
        job.join()

        assertFalse(job.isCancelled)
        assertTrue(dao.getPurchasesForLine(lineId).isEmpty())
    }

    private fun purchase(lineId: Long) = ToppingPurchase(
        lineId = lineId,
        purchaseDate = LocalDate.of(2026, 9, 1),
        toppingName = "データ使い放題(24時間)",
    )
}

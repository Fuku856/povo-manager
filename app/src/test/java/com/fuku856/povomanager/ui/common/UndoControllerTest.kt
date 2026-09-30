package com.fuku856.povomanager.ui.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

/** [UndoController] の検証。失敗時のログ出力(android.util.Log)のため Robolectric で動かす。 */
@RunWith(AndroidJUnit4::class)
class UndoControllerTest {
    // 取り消し処理から漏れた例外を記録する(本番ではアプリごと落ちる)
    private val uncaught = Collections.synchronizedList(mutableListOf<Throwable>())
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> uncaught += e },
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun show_replacesCurrentRequest() {
        val controller = UndoController(scope, timeoutMillis = 60_000)

        controller.show("A") {}
        val first = controller.current.value!!
        controller.show("B") {}

        assertEquals("B", controller.current.value!!.message)
        // 置き換わった古い要求を閉じても、新しい要求は消えない
        controller.onClosed(first, undoRequested = false)
        assertEquals("B", controller.current.value!!.message)
    }

    @Test
    fun onClosed_withUndo_runsUndoAndClears() = runBlocking {
        val controller = UndoController(scope, timeoutMillis = 60_000)
        val undone = CompletableDeferred<Unit>()

        controller.show("A") { undone.complete(Unit) }
        controller.onClosed(controller.current.value!!, undoRequested = true)

        assertNull(controller.current.value)
        withTimeout(1_000) { undone.await() }
    }

    @Test
    fun onClosed_withoutUndo_doesNotRunUndo() = runBlocking {
        val controller = UndoController(scope, timeoutMillis = 60_000)
        var undone = false

        controller.show("A") { undone = true }
        controller.onClosed(controller.current.value!!, undoRequested = false)
        delay(100)

        assertNull(controller.current.value)
        assertFalse(undone)
    }

    @Test
    fun request_expiresAfterTimeout_withoutRunningUndo() = runBlocking {
        val controller = UndoController(scope, timeoutMillis = 100)
        var undone = false

        controller.show("A") { undone = true }
        delay(400)

        assertNull(controller.current.value)
        assertFalse(undone)
    }

    @Test
    fun expiryOfOldRequest_doesNotClearNewerRequest() = runBlocking {
        val controller = UndoController(scope, timeoutMillis = 300)

        controller.show("A") {}
        delay(200)
        controller.show("B") {}
        val second = controller.current.value
        delay(200) // A の期限(300ms)は過ぎたが、B の期限はまだ

        assertSame(second, controller.current.value)
    }

    @Test
    fun failingUndo_doesNotLeakException() = runBlocking {
        val controller = UndoController(scope, timeoutMillis = 60_000)

        // 例: トースト表示中に回線が削除され、購入の復元が外部キー違反で失敗する
        controller.show("A") { error("FOREIGN KEY constraint failed") }
        controller.onClosed(controller.current.value!!, undoRequested = true)
        delay(300)

        assertTrue(uncaught.isEmpty())
    }
}

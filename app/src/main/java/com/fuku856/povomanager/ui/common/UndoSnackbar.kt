package com.fuku856.povomanager.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * [UndoController] の要求を [hostState] にトーストとして表示する。
 * 画面遷移で消えないよう、NavHost の外(アプリ全体)で1回だけ呼ぶ。
 *
 * 表示時間は [UndoController] が管理する。要求が置き換わる・期限切れで消えると LaunchedEffect が
 * キャンセルされ、表示中のトーストも閉じる。画面回転などで作り直されたときは、残っている要求を出し直す。
 */
@Composable
fun UndoSnackbarEffect(controller: UndoController, hostState: SnackbarHostState) {
    val request by controller.current.collectAsState()
    LaunchedEffect(request) {
        val current = request ?: return@LaunchedEffect
        val result = hostState.showSnackbar(
            current.message,
            actionLabel = current.actionLabel,
            duration = SnackbarDuration.Indefinite,
        )
        // スワイプで閉じた場合は Dismissed
        controller.onClosed(current, undoRequested = result == SnackbarResult.ActionPerformed)
    }
}

package com.fuku856.povomanager.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.withTimeoutOrNull

/** 操作後の「取り消す」トーストが自動的に消えるまでの時間(ミリ秒)。 */
const val UNDO_SNACKBAR_TIMEOUT_MS = 5_000L

/**
 * 「取り消す」などアクション付きのスナックバーを表示する。
 *
 * Material3 の [SnackbarHostState.showSnackbar] は actionLabel を渡すと duration が
 * 既定で [SnackbarDuration.Indefinite] になり、手動で閉じるまで残り続ける。本関数は
 * [timeoutMillis] 経過で自動的に閉じる(スワイプで閉じた場合も含め [SnackbarResult.Dismissed])。
 * アクションがタップされた場合のみ [SnackbarResult.ActionPerformed] を返す。
 *
 * 表示中のスナックバーがあれば閉じてから出す(置き換え)。既定の [showSnackbar] は前のものが
 * 消えるまで待つため、連続操作すると古い操作のトーストが残り、「取り消す」が直前の操作ではなく
 * 古い操作に効いてしまう。呼び出し側も collectLatest で受け、同じイベント列の前の表示を打ち切ること。
 */
suspend fun SnackbarHostState.showUndoSnackbar(
    message: String,
    actionLabel: String,
    timeoutMillis: Long = UNDO_SNACKBAR_TIMEOUT_MS,
): SnackbarResult {
    currentSnackbarData?.dismiss()
    return withTimeoutOrNull(timeoutMillis) {
        showSnackbar(message, actionLabel = actionLabel, duration = SnackbarDuration.Indefinite)
    } ?: SnackbarResult.Dismissed
}

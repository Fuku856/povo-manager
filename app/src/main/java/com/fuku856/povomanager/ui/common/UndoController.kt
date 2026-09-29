package com.fuku856.povomanager.ui.common

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** 操作後の「取り消す」トーストが自動的に消えるまでの時間(ミリ秒)。 */
const val UNDO_SNACKBAR_TIMEOUT_MS = 5_000L

/**
 * 「取り消す」トーストの要求。[undo] は取り消しが選ばれたときに実行する処理。
 * 同一性で比較する(同じ文言の要求が続いても別の要求として扱う)。
 */
class UndoRequest internal constructor(
    val message: String,
    val actionLabel: String,
    internal val undo: suspend () -> Unit,
)

/**
 * 「取り消す」トーストの要求をアプリ全体で1つだけ保持する。表示は [UndoSnackbarEffect]。
 *
 * 要求を画面(コンポジション)や ViewModel ではなくアプリ寿命のシングルトンに置くことで、
 * - 画面回転などで Activity が作り直されても、残り時間のあいだトーストを出し直せる
 * - 操作した画面から戻っても(ViewModel が破棄されても)トーストが残り、取り消しも実行できる
 *
 * 新しい要求は表示中の要求を置き換える(前の操作は取り消せなくなる)。表示されていなくても
 * [timeoutMillis] で期限切れになるため、後から画面に戻ったときに古いトーストは出ない。
 */
@Singleton
class UndoController internal constructor(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long,
) {
    // 取り消し処理は、呼び出し元の画面・ViewModel が破棄された後でも走り切らせる
    @Inject
    constructor() : this(CoroutineScope(SupervisorJob() + Dispatchers.Default), UNDO_SNACKBAR_TIMEOUT_MS)

    private val _current = MutableStateFlow<UndoRequest?>(null)
    val current: StateFlow<UndoRequest?> = _current.asStateFlow()

    fun show(message: String, actionLabel: String = "取り消す", undo: suspend () -> Unit) {
        val request = UndoRequest(message, actionLabel, undo)
        _current.value = request
        scope.launch {
            delay(timeoutMillis)
            // 既に次の要求に置き換わっていたら、そちらは消さない
            _current.compareAndSet(request, null)
        }
    }

    /** トーストが閉じられたときに呼ぶ。[undoRequested] なら取り消しを実行する。 */
    fun onClosed(request: UndoRequest, undoRequested: Boolean) {
        // 既に次の要求に置き換わっていたら、そちらは消さない
        _current.compareAndSet(request, null)
        if (!undoRequested) return
        scope.launch {
            // 取り消しはトーストの表示中に変わった状態(回線の削除など)に対して走ることがある。
            // 失敗してもアプリ寿命のスコープで例外を投げてアプリごと落とさない。
            runCatching { request.undo() }
                .onFailure { Log.w(TAG, "取り消しに失敗しました", it) }
        }
    }

    private companion object {
        const val TAG = "UndoController"
    }
}

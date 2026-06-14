package com.fuku856.povomanager.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // ウィジェット更新は呼び出し元(viewModelScope 等)のライフサイクルから切り離す。
    // 並び替え直後に設定画面を離れて呼び出し元スコープがキャンセルされても、更新が
    // 打ち切られず確実に完了するよう、アプリ寿命の専用スコープで実行する。
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 更新を直列化する。削除→元に戻す のように短時間に複数回呼ばれたとき、並行実行による
    // 完了順の前後で古い状態(例: 履歴なし)が最後に確定してしまうのを防ぐ。
    private val mutex = Mutex()

    /**
     * 全ウィジェットを最新のDB状態で更新する。
     *
     * 実体は専用スコープ上のジョブとして起動するため、呼び出し元がキャンセルされても更新は完了する。
     * join() で待つので、Worker など完了を待ちたい呼び出し元はそのまま await できる(呼び出し元が
     * キャンセルされた場合は join がほどけるだけで、ジョブ自体は走り切る)。
     */
    suspend fun updateAll() {
        scope.launch {
            mutex.withLock {
                runCatching { PovoWidget().updateAll(context) }
                    .onFailure { Log.w(TAG, "ウィジェット更新に失敗しました", it) }
            }
        }.join()
    }

    private companion object {
        const val TAG = "WidgetUpdater"
    }
}

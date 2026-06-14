package com.fuku856.povomanager.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    // 並び替え直後に画面を離れて呼び出し元スコープがキャンセルされても、更新が
    // 打ち切られず確実に完了するよう、アプリ寿命の専用スコープで実行する。
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 更新要求のコンフレーション(まとめ実行)用の状態。すべて mutex 下で操作する。
    private val mutex = Mutex()
    private var requestedGen = 0L          // updateAll が呼ばれるたびに増える要求世代
    private var renderJob: Job? = null     // 実行中のレンダリングループ(無ければ null)
    // 描画が完了した世代。ここまで進めば、その世代以前の要求時点の状態は反映済み。
    private val completedGen = MutableStateFlow(0L)

    /**
     * 全ウィジェットを最新のDB状態で更新する。
     *
     * 短時間に複数回呼ばれても、中間の描画は省いて最新状態へ最小回数で追従する(コンフレーション)。
     * 自分の要求時点以降の状態を反映した描画が完了するまで suspend するため、Worker など
     * 完了を待ちたい呼び出し元はそのまま await できる。実体はアプリ寿命スコープのジョブなので、
     * 呼び出し元がキャンセルされても描画自体は走り切る(await がほどけるだけ)。
     */
    suspend fun updateAll() {
        val myGen = mutex.withLock {
            val gen = ++requestedGen
            if (renderJob?.isActive != true) {
                renderJob = scope.launch { renderLoop() }
            }
            gen
        }
        // 自分の要求(myGen)を反映した描画が完了するまで待つ。
        completedGen.first { it >= myGen }
    }

    /** 未処理の要求がある間だけ描画を繰り返す。バースト分は1〜2回の描画にまとめられる。 */
    private suspend fun renderLoop() {
        while (true) {
            val gen = mutex.withLock {
                val latest = requestedGen
                if (latest <= completedGen.value) {
                    renderJob = null // 未処理の要求が無いのでループ終了。次回はまた起動される。
                    null
                } else {
                    latest
                }
            } ?: return
            // 描画は実行時に最新DBを読むため、gen 以前の要求はすべて反映される。
            runCatching { PovoWidget().updateAll(context) }
                .onFailure { Log.w(TAG, "ウィジェット更新に失敗しました", it) }
            completedGen.value = gen
        }
    }

    private companion object {
        const val TAG = "WidgetUpdater"
    }
}

package com.fuku856.povomanager.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // 更新を直列化する。削除→元に戻す のように短時間に複数回呼ばれたとき、並行実行による
    // 完了順の前後で古い状態(例: 履歴なし)が最後に確定してしまうのを防ぐ。
    private val mutex = Mutex()

    suspend fun updateAll() {
        mutex.withLock {
            runCatching { PovoWidget().updateAll(context) }
        }
    }
}

package com.fuku856.povomanager.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.backup.BackupManager
import com.fuku856.povomanager.data.backup.ImportMode
import com.fuku856.povomanager.data.backup.ImportPreviewLine
import com.fuku856.povomanager.data.settings.AppSettings
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.toStatus
import com.fuku856.povomanager.notifications.NotificationScheduler
import com.fuku856.povomanager.ui.common.displayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** インポート確定前のプレビュー状態(選択済みのモードと取り込まれる回線一覧) */
data class ImportPreview(
    val uri: Uri,
    val mode: ImportMode,
    val lines: List<ImportPreviewLine>,
)

/** ウィジェット並び替えUI用の軽量な回線モデル */
data class WidgetLineRow(
    val id: Long,
    val name: String,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationScheduler: NotificationScheduler,
    private val backupManager: BackupManager,
    private val lineRepository: LineRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** ウィジェット手動並び替え用の回線一覧(DAOのsortOrder順で流れてくる) */
    val widgetLines: StateFlow<List<WidgetLineRow>> = lineRepository.observeLinesWithPurchases()
        .map { list -> list.map { WidgetLineRow(it.line.id, it.line.displayName) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    /** 非nullのときインポート確定前のプレビューを表示する */
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.exportTo(uri) }
                .onSuccess { count -> _messages.send("${count}回線をエクスポートしました") }
                .onFailure { _messages.send("エクスポートに失敗しました: ${it.message}") }
        }
    }

    /** インポート方法の選択後、ファイルを読み込んでプレビューを表示する。 */
    fun prepareImport(uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            runCatching { backupManager.preview(uri) }
                .onSuccess { lines -> _importPreview.value = ImportPreview(uri, mode, lines) }
                .onFailure { _messages.send("ファイルを読み込めませんでした。形式を確認してください") }
        }
    }

    /** プレビューを破棄してインポートを中止する。 */
    fun cancelImport() {
        _importPreview.value = null
    }

    /** プレビュー中の内容を実際にインポートする。 */
    fun confirmImport() {
        val preview = _importPreview.value ?: return
        _importPreview.value = null
        importFrom(preview.uri, preview.mode)
    }

    private fun importFrom(uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            runCatching { backupManager.importFrom(uri, mode) }
                .onSuccess { count ->
                    val verb = if (mode == ImportMode.REPLACE) "置き換えました" else "取り込みました"
                    _messages.send("${count}回線を$verb")
                }
                .onFailure { _messages.send("インポートに失敗しました。ファイル形式を確認してください") }
        }
    }

    fun toggleNotifyDay(day: Int) {
        viewModelScope.launch {
            val current = settingsRepository.current().defaultNotifyDays
            settingsRepository.setDefaultNotifyDays(if (day in current) current - day else current + day)
        }
    }

    fun toggleToppingNotifyDay(day: Int) {
        viewModelScope.launch {
            val current = settingsRepository.current().toppingExpiryNotifyDays
            settingsRepository.setToppingExpiryNotifyDays(if (day in current) current - day else current + day)
        }
    }

    fun setNotifyTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setNotifyTime(hour, minute)
            // 通知時刻の変更時は既存スケジュールを置き換える
            notificationScheduler.schedule(hour, minute, replace = true)
        }
    }

    fun setExpiryPeriodDays(days: Int) {
        viewModelScope.launch { settingsRepository.setExpiryPeriodDays(days) }
    }

    fun setWidgetManualOrder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWidgetManualOrder(enabled)
            // 初回ON時、sortOrderが未設定(全件同値)なら現在の「期限の早い順」をシードして、
            // 手動リストの初期並びが直前のウィジェット表示と一致するようにする。
            if (enabled) {
                val lines = lineRepository.getLinesWithPurchases()
                if (lines.size > 1 && lines.map { it.line.sortOrder }.distinct().size <= 1) {
                    val settings = settingsRepository.current()
                    val today = LocalDate.now()
                    val orderedIds = lines
                        .map { it.toStatus(settings, today) }
                        .sortedWith(compareBy(nullsLast()) { it.daysRemaining })
                        .map { it.line.id }
                    lineRepository.setLineOrder(orderedIds)
                }
            }
        }
    }

    /** ドラッグ&ドロップ確定後の並び順を保存する。 */
    fun commitWidgetOrder(orderedIds: List<Long>) {
        viewModelScope.launch { lineRepository.setLineOrder(orderedIds) }
    }
}

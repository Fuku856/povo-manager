package com.fuku856.povomanager.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.domain.toStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ArchivedUiState(
    val statuses: List<LineStatus> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class ArchivedLinesViewModel @Inject constructor(
    private val repository: LineRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ArchivedUiState> =
        combine(
            repository.observeArchivedLinesWithPurchases(),
            settingsRepository.settings,
        ) { lines, settings ->
            val today = LocalDate.now()
            ArchivedUiState(
                statuses = lines
                    .map { it.toStatus(settings, today) }
                    .sortedWith(compareBy(nullsLast()) { it.daysRemaining }),
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchivedUiState())

    /** 取り消し用にアーカイブ解除した回線IDを流すイベント */
    private val _unarchivedEvent = Channel<Long>(Channel.BUFFERED)
    val unarchivedEvent = _unarchivedEvent.receiveAsFlow()

    fun unarchive(lineId: Long) {
        viewModelScope.launch {
            val line = repository.getLine(lineId) ?: return@launch
            // 解除ボタンの連打などで二重に呼ばれてもトーストを重ねない
            if (!line.isArchived) return@launch
            repository.setArchived(line, false)
            _unarchivedEvent.send(lineId)
        }
    }

    /** 取り消し。スナップショットではなく最新を取り直し、アーカイブ状態だけ戻す */
    fun rearchive(lineId: Long) {
        viewModelScope.launch {
            val line = repository.getLine(lineId) ?: return@launch
            repository.setArchived(line, true)
        }
    }
}

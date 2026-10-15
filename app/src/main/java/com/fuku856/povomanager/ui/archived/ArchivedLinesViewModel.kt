package com.fuku856.povomanager.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.domain.toStatusesByExpiry
import com.fuku856.povomanager.ui.common.UndoController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class ArchivedUiState(
    val statuses: List<LineStatus> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class ArchivedLinesViewModel @Inject constructor(
    private val repository: LineRepository,
    settingsRepository: SettingsRepository,
    private val undoController: UndoController,
) : ViewModel() {

    val uiState: StateFlow<ArchivedUiState> =
        combine(
            repository.observeArchivedLinesWithPurchases(),
            settingsRepository.settings,
        ) { lines, settings ->
            ArchivedUiState(
                statuses = lines.toStatusesByExpiry(settings, LocalDateTime.now()),
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchivedUiState())

    fun unarchive(lineId: Long) {
        viewModelScope.launch {
            // 連打などで二重に呼ばれても、状態が実際に変わったときだけトーストを出す
            if (!repository.setArchived(lineId, false)) return@launch
            undoController.show("アーカイブを解除しました") { repository.setArchived(lineId, true) }
        }
    }
}

package com.fuku856.povomanager.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.domain.toStatusesByExpiry
import com.fuku856.povomanager.ui.common.UndoController
import com.fuku856.povomanager.ui.common.UndoableAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val undoController: UndoController,
) : ViewModel() {

    val uiState: StateFlow<ArchivedUiState> =
        combine(
            repository.observeArchivedLinesWithPurchases(),
            settingsRepository.settings,
        ) { lines, settings ->
            ArchivedUiState(
                statuses = lines.toStatusesByExpiry(settings, LocalDate.now()),
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchivedUiState())

    fun unarchive(lineId: Long) {
        viewModelScope.launch {
            val line = repository.getLine(lineId) ?: return@launch
            // 解除ボタンの連打などで二重に呼ばれてもトーストを重ねない
            if (!line.isArchived) return@launch
            repository.setArchived(lineId, false)
            undoController.show(UndoableAction.ArchiveChanged(lineId, archived = false))
        }
    }
}

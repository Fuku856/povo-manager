package com.fuku856.povomanager.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.settings.AppSettings
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.domain.toStatus
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
    val expiryPeriodDays: Int = AppSettings.DEFAULT_EXPIRY_PERIOD_DAYS,
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
                expiryPeriodDays = settings.expiryPeriodDays,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchivedUiState())

    fun unarchive(lineId: Long) {
        viewModelScope.launch {
            val line = repository.getLine(lineId) ?: return@launch
            repository.setArchived(line, false)
        }
    }
}

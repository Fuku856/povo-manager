package com.fuku856.povomanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
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

data class HomeUiState(
    val statuses: List<LineStatus> = emptyList(),
    val expiryPeriodDays: Int = AppSettings.DEFAULT_EXPIRY_PERIOD_DAYS,
    /** アーカイブ済み回線数。「アーカイブ済みを表示」ボタンの表示判定に使う */
    val archivedCount: Int = 0,
    val loaded: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LineRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeActiveLinesWithPurchases(),
            repository.observeArchivedLinesWithPurchases(),
            settingsRepository.settings,
        ) { lines, archived, settings ->
            val today = LocalDate.now()
            HomeUiState(
                statuses = lines
                    .map { it.toStatus(settings, today) }
                    .sortedWith(compareBy(nullsLast()) { it.daysRemaining }),
                expiryPeriodDays = settings.expiryPeriodDays,
                archivedCount = archived.size,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** 取り消し用に追加した購入を流すイベント */
    private val _purchaseAdded = Channel<ToppingPurchase>(Channel.BUFFERED)
    val purchaseAdded = _purchaseAdded.receiveAsFlow()

    /** 取り消し用にアーカイブした回線を流すイベント */
    private val _archivedEvent = Channel<PovoLine>(Channel.BUFFERED)
    val archivedEvent = _archivedEvent.receiveAsFlow()

    fun recordPurchase(lineId: Long, date: LocalDate, toppingName: String, validityEndDate: LocalDate?) {
        viewModelScope.launch {
            val purchase = ToppingPurchase(
                lineId = lineId,
                purchaseDate = date,
                toppingName = toppingName,
                validityEndDate = validityEndDate,
            )
            val id = repository.addPurchase(purchase)
            _purchaseAdded.send(purchase.copy(id = id))
        }
    }

    fun undoPurchase(purchase: ToppingPurchase) {
        viewModelScope.launch { repository.deletePurchase(purchase) }
    }

    fun archiveLine(lineId: Long) {
        viewModelScope.launch {
            val line = repository.getLine(lineId) ?: return@launch
            repository.setArchived(line, true)
            _archivedEvent.send(line)
        }
    }

    fun unarchive(line: PovoLine) {
        viewModelScope.launch { repository.setArchived(line, false) }
    }
}

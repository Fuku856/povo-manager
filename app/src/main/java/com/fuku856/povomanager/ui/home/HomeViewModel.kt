package com.fuku856.povomanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
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
    private val undoController: UndoController,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeActiveLinesWithPurchases(),
            repository.observeArchivedCount(),
            settingsRepository.settings,
        ) { lines, archivedCount, settings ->
            HomeUiState(
                statuses = lines.toStatusesByExpiry(settings, LocalDate.now()),
                expiryPeriodDays = settings.expiryPeriodDays,
                archivedCount = archivedCount,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun recordPurchase(lineId: Long, date: LocalDate, toppingName: String, validityEndDate: LocalDate?) {
        viewModelScope.launch {
            val purchase = ToppingPurchase(
                lineId = lineId,
                purchaseDate = date,
                toppingName = toppingName,
                validityEndDate = validityEndDate,
            )
            val added = purchase.copy(id = repository.addPurchase(purchase))
            undoController.show("購入を記録しました") { repository.deletePurchase(added) }
        }
    }

    fun archiveLine(lineId: Long) {
        viewModelScope.launch {
            // 連打などで二重に呼ばれても、状態が実際に変わったときだけトーストを出す
            if (!repository.setArchived(lineId, true)) return@launch
            undoController.show("アーカイブしました") { repository.setArchived(lineId, false) }
        }
    }
}

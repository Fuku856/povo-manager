package com.fuku856.povomanager.ui.linedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.domain.toStatus
import com.fuku856.povomanager.ui.LineDetailRoute
import com.fuku856.povomanager.ui.common.UndoController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LineDetailUiState(
    val loaded: Boolean = false,
    /** nullなら回線が存在しない(削除済み) */
    val status: LineStatus? = null,
    val expiryPeriodDays: Int = AppSettings.DEFAULT_EXPIRY_PERIOD_DAYS,
)

@HiltViewModel
class LineDetailViewModel @Inject constructor(
    private val repository: LineRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
    private val undoController: UndoController,
) : ViewModel() {

    private val lineId: Long = savedStateHandle.toRoute<LineDetailRoute>().lineId

    val uiState: StateFlow<LineDetailUiState> =
        combine(repository.observeLineWithPurchases(lineId), settingsRepository.settings) { line, settings ->
            LineDetailUiState(
                loaded = true,
                status = line?.toStatus(settings, LocalDate.now()),
                expiryPeriodDays = settings.expiryPeriodDays,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LineDetailUiState())

    fun recordPurchase(date: LocalDate, toppingName: String, validityEndDate: LocalDate?) {
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

    fun updatePurchase(original: ToppingPurchase, date: LocalDate, toppingName: String, validityEndDate: LocalDate?) {
        viewModelScope.launch {
            repository.updatePurchase(
                original.copy(purchaseDate = date, toppingName = toppingName, validityEndDate = validityEndDate)
            )
        }
    }

    /** アーカイブ状態をトグルし、取り消しトーストを出す。 */
    fun toggleArchive() {
        viewModelScope.launch {
            val line = repository.getLine(lineId) ?: return@launch
            val nowArchived = !line.isArchived
            if (!repository.setArchived(lineId, nowArchived)) return@launch
            undoController.show(if (nowArchived) "アーカイブしました" else "アーカイブを解除しました") {
                repository.setArchived(lineId, !nowArchived)
            }
        }
    }

    fun deletePurchase(purchase: ToppingPurchase) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
            undoController.show("履歴を削除しました", actionLabel = "元に戻す") {
                // トーストが残っている間に回線ごと削除されていたら戻さない(外部キー違反になるため)
                if (repository.getLine(purchase.lineId) != null) {
                    repository.addPurchase(purchase.copy(id = 0))
                }
            }
        }
    }
}

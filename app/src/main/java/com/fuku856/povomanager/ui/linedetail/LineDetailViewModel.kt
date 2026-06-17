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

data class LineDetailUiState(
    val loaded: Boolean = false,
    /** nullなら回線が存在しない(削除済み) */
    val status: LineStatus? = null,
    val expiryPeriodDays: Int = AppSettings.DEFAULT_EXPIRY_PERIOD_DAYS,
)

sealed interface PurchaseEvent {
    data class Added(val purchase: ToppingPurchase) : PurchaseEvent
    data class Deleted(val purchase: ToppingPurchase) : PurchaseEvent
}

@HiltViewModel
class LineDetailViewModel @Inject constructor(
    private val repository: LineRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
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

    private val _events = Channel<PurchaseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun recordPurchase(date: LocalDate, toppingName: String, validityEndDate: LocalDate?) {
        viewModelScope.launch {
            val purchase = ToppingPurchase(
                lineId = lineId,
                purchaseDate = date,
                toppingName = toppingName,
                validityEndDate = validityEndDate,
            )
            val id = repository.addPurchase(purchase)
            _events.send(PurchaseEvent.Added(purchase.copy(id = id)))
        }
    }

    fun updatePurchase(original: ToppingPurchase, date: LocalDate, toppingName: String, validityEndDate: LocalDate?) {
        viewModelScope.launch {
            repository.updatePurchase(
                original.copy(purchaseDate = date, toppingName = toppingName, validityEndDate = validityEndDate)
            )
        }
    }

    fun toggleArchive() {
        viewModelScope.launch {
            val line = repository.getLine(lineId) ?: return@launch
            repository.setArchived(line, !line.isArchived)
        }
    }

    fun deletePurchase(purchase: ToppingPurchase) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
            _events.send(PurchaseEvent.Deleted(purchase))
        }
    }

    fun restorePurchase(purchase: ToppingPurchase) {
        viewModelScope.launch { repository.addPurchase(purchase.copy(id = 0)) }
    }

    fun undoPurchase(purchase: ToppingPurchase) {
        viewModelScope.launch { repository.deletePurchase(purchase) }
    }
}

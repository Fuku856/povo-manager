package com.fuku856.povomanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.domain.ValidityEnd
import com.fuku856.povomanager.domain.toStatusesByExpiry
import com.fuku856.povomanager.ui.common.UndoController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HomeUiState(
    val statuses: List<LineStatus> = emptyList(),
    val expiryPeriodDays: Int = AppSettings.DEFAULT_EXPIRY_PERIOD_DAYS,
    /** アーカイブ済み回線数。「アーカイブ済みを表示」ボタンの表示判定に使う */
    val archivedCount: Int = 0,
    /** 表示の基準時刻(トッピングの残り時間表示に使う)。分が変わるたびに更新される */
    val now: LocalDateTime = LocalDateTime.now(),
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
            minuteTicker(),
        ) { lines, archivedCount, settings, now ->
            HomeUiState(
                statuses = lines.toStatusesByExpiry(settings, now),
                expiryPeriodDays = settings.expiryPeriodDays,
                archivedCount = archivedCount,
                now = now,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun recordPurchase(lineId: Long, purchasedAt: LocalDateTime, toppingName: String, validityEnd: ValidityEnd?) {
        viewModelScope.launch {
            val purchase = ToppingPurchase(
                lineId = lineId,
                purchaseDate = purchasedAt.toLocalDate(),
                purchaseTime = purchasedAt.toLocalTime(),
                toppingName = toppingName,
                validityEndDate = validityEnd?.date,
                validityEndTime = validityEnd?.time,
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

/**
 * 現在時刻を流し、以後は分が変わるたびに流し直す。
 * トッピングの残り時間表示と、満了した時点での有効中トッピングの切り替えに使う。
 */
private fun minuteTicker(): Flow<LocalDateTime> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now)
        val nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        delay(Duration.between(now, nextMinute).toMillis().coerceAtLeast(1))
    }
}

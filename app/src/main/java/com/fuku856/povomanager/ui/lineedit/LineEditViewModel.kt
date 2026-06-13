package com.fuku856.povomanager.ui.lineedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.data.settings.AppSettings
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.ui.LineEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LineEditUiState(
    val isNew: Boolean = true,
    val loaded: Boolean = false,
    val phoneNumber: String = "",
    val name: String = "",
    val memo: String = "",
    val overrideEnabled: Boolean = false,
    val overrideDays: Set<Int> = emptySet(),
    val defaultNotifyDays: Set<Int> = AppSettings.DEFAULT_NOTIFY_DAYS,
    /** 新規登録時のみ: 最終トッピング購入日を同時に記録する */
    val recordInitialPurchase: Boolean = true,
    val initialPurchaseDate: LocalDate = LocalDate.now(),
    val phoneError: String? = null,
)

@HiltViewModel
class LineEditViewModel @Inject constructor(
    private val repository: LineRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val lineId: Long = savedStateHandle.toRoute<LineEditRoute>().lineId
    private var existingLine: PovoLine? = null

    private val _uiState = MutableStateFlow(LineEditUiState(isNew = lineId == -1L))
    val uiState: StateFlow<LineEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val line = if (lineId != -1L) repository.getLine(lineId) else null
            existingLine = line
            _uiState.update {
                it.copy(
                    loaded = true,
                    defaultNotifyDays = settings.defaultNotifyDays,
                    phoneNumber = line?.phoneNumber ?: "",
                    name = line?.name ?: "",
                    memo = line?.memo ?: "",
                    overrideEnabled = line?.notifyDaysOverride != null,
                    overrideDays = line?.notifyDaysOverride ?: settings.defaultNotifyDays,
                )
            }
        }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value.filter(Char::isDigit).take(11), phoneError = null) }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun onMemoChange(value: String) = _uiState.update { it.copy(memo = value) }

    fun onOverrideEnabledChange(enabled: Boolean) = _uiState.update { it.copy(overrideEnabled = enabled) }

    fun onOverrideDayToggle(day: Int) = _uiState.update {
        val days = if (day in it.overrideDays) it.overrideDays - day else it.overrideDays + day
        it.copy(overrideDays = days)
    }

    fun onRecordInitialPurchaseChange(enabled: Boolean) =
        _uiState.update { it.copy(recordInitialPurchase = enabled) }

    fun onInitialPurchaseDateChange(date: LocalDate) =
        _uiState.update { it.copy(initialPurchaseDate = date) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val phone = state.phoneNumber
        if (phone.length !in 10..11 || !phone.startsWith("0")) {
            _uiState.update { it.copy(phoneError = "0から始まる10〜11桁の電話番号を入力してください") }
            return
        }
        viewModelScope.launch {
            val line = (existingLine ?: PovoLine(phoneNumber = phone)).copy(
                phoneNumber = phone,
                name = state.name.trim().ifBlank { null },
                memo = state.memo.trim().ifBlank { null },
                notifyDaysOverride = if (state.overrideEnabled) state.overrideDays else null,
            )
            if (state.isNew) {
                val newId = repository.addLine(line)
                if (state.recordInitialPurchase) {
                    repository.addPurchase(
                        ToppingPurchase(
                            lineId = newId,
                            purchaseDate = state.initialPurchaseDate,
                            toppingName = "初回登録(最終購入日)",
                        )
                    )
                }
            } else {
                repository.updateLine(line)
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val line = existingLine ?: return
        viewModelScope.launch {
            repository.deleteLine(line)
            onDeleted()
        }
    }
}

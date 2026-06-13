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
import kotlinx.coroutines.flow.updateAndGet
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

    private val handle = savedStateHandle

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val line = if (lineId != -1L) repository.getLine(lineId) else null
            existingLine = line
            if (handle.get<Boolean>(KEY_DRAFT) == true) {
                // プロセス再生成: 入力中の下書きを復元する(DB値で上書きしない)
                _uiState.update {
                    it.copy(
                        loaded = true,
                        defaultNotifyDays = settings.defaultNotifyDays,
                        phoneNumber = handle[KEY_PHONE] ?: "",
                        name = handle[KEY_NAME] ?: "",
                        memo = handle[KEY_MEMO] ?: "",
                        overrideEnabled = handle[KEY_OVERRIDE_ENABLED] ?: false,
                        overrideDays = (handle.get<IntArray>(KEY_OVERRIDE_DAYS) ?: IntArray(0)).toSet(),
                        recordInitialPurchase = handle[KEY_RECORD_INITIAL] ?: true,
                        initialPurchaseDate = handle.get<Long>(KEY_INITIAL_DATE)
                            ?.let(LocalDate::ofEpochDay) ?: LocalDate.now(),
                    )
                }
            } else {
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
    }

    /** _uiState を更新し、入力中の下書きを SavedStateHandle に保存する */
    private inline fun updateDraft(block: (LineEditUiState) -> LineEditUiState) {
        val newState = _uiState.updateAndGet(block)
        handle[KEY_DRAFT] = true
        handle[KEY_PHONE] = newState.phoneNumber
        handle[KEY_NAME] = newState.name
        handle[KEY_MEMO] = newState.memo
        handle[KEY_OVERRIDE_ENABLED] = newState.overrideEnabled
        handle[KEY_OVERRIDE_DAYS] = newState.overrideDays.toIntArray()
        handle[KEY_RECORD_INITIAL] = newState.recordInitialPurchase
        handle[KEY_INITIAL_DATE] = newState.initialPurchaseDate.toEpochDay()
    }

    fun onPhoneChange(value: String) =
        updateDraft { it.copy(phoneNumber = value.filter(Char::isDigit).take(11), phoneError = null) }

    fun onNameChange(value: String) = updateDraft { it.copy(name = value) }

    fun onMemoChange(value: String) = updateDraft { it.copy(memo = value) }

    fun onOverrideEnabledChange(enabled: Boolean) = updateDraft { it.copy(overrideEnabled = enabled) }

    fun onOverrideDayToggle(day: Int) = updateDraft {
        val days = if (day in it.overrideDays) it.overrideDays - day else it.overrideDays + day
        it.copy(overrideDays = days)
    }

    fun onRecordInitialPurchaseChange(enabled: Boolean) =
        updateDraft { it.copy(recordInitialPurchase = enabled) }

    fun onInitialPurchaseDateChange(date: LocalDate) =
        updateDraft { it.copy(initialPurchaseDate = date) }

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
            handle[KEY_DRAFT] = false // 保存完了後は下書きを破棄
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val line = existingLine ?: return
        viewModelScope.launch {
            repository.deleteLine(line)
            handle[KEY_DRAFT] = false
            onDeleted()
        }
    }

    private companion object {
        const val KEY_DRAFT = "draft_active"
        const val KEY_PHONE = "draft_phone"
        const val KEY_NAME = "draft_name"
        const val KEY_MEMO = "draft_memo"
        const val KEY_OVERRIDE_ENABLED = "draft_override_enabled"
        const val KEY_OVERRIDE_DAYS = "draft_override_days"
        const val KEY_RECORD_INITIAL = "draft_record_initial"
        const val KEY_INITIAL_DATE = "draft_initial_date"
    }
}

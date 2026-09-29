package com.fuku856.povomanager.ui.common

import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.db.ToppingPurchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 取り消しトーストで元に戻せる操作。
 * 取り消し処理は [UndoController] が repository を直接呼んで行うため、操作した画面の ViewModel を必要としない。
 */
sealed interface UndoableAction {
    val message: String
    val actionLabel: String get() = "取り消す"

    /** 購入の記録。取り消すと記録した購入を削除する */
    data class PurchaseAdded(val purchase: ToppingPurchase) : UndoableAction {
        override val message get() = "購入を記録しました"
    }

    /** 購入履歴の削除。元に戻すと同じ内容で追加し直す */
    data class PurchaseDeleted(val purchase: ToppingPurchase) : UndoableAction {
        override val message get() = "履歴を削除しました"
        override val actionLabel get() = "元に戻す"
    }

    /** アーカイブ状態の変更。[archived] は変更後の状態で、取り消すと逆の状態に戻す */
    data class ArchiveChanged(val lineId: Long, val archived: Boolean) : UndoableAction {
        override val message get() = if (archived) "アーカイブしました" else "アーカイブを解除しました"
    }
}

/**
 * アプリ全体で1つの取り消しトーストを受け持つ。
 *
 * 各画面の ViewModel は操作後に [show] で取り消しトーストを依頼し、PovoApp が NavHost の外側の
 * スナックバーに表示する。そのため画面を移ってもトーストは残る。「取り消す」が押されたら [undo] で
 * 元に戻す。取り消しはアプリ寿命のスコープで repository を直接呼ぶので、操作した画面を離れて
 * ViewModel が破棄されたあとでも実行できる。
 */
@Singleton
class UndoController @Inject constructor(
    private val repository: LineRepository,
) {
    // 取り消しは操作した画面(viewModelScope)のライフサイクルから切り離し、アプリ寿命のスコープで実行する。
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 購読者(PovoApp)がいない間の依頼は破棄し、あとから古い取り消しトーストを出さない。
    private val _requests = MutableSharedFlow<UndoableAction>(extraBufferCapacity = 16)

    /**
     * 取り消しトーストの表示依頼。受け取る側は collectLatest で受け、新しい依頼が来たら
     * 表示中のトーストを置き換えること(「取り消す」を常に直前の操作に効かせるため)。
     */
    val requests: SharedFlow<UndoableAction> = _requests.asSharedFlow()

    /** 操作を実行した直後に呼び、取り消しトーストの表示を依頼する */
    fun show(action: UndoableAction) {
        _requests.tryEmit(action)
    }

    /** トーストの「取り消す」「元に戻す」が押されたときに呼ぶ */
    fun undo(action: UndoableAction): Job = scope.launch {
        when (action) {
            is UndoableAction.PurchaseAdded -> repository.deletePurchase(action.purchase)
            is UndoableAction.PurchaseDeleted -> {
                // トーストが残っている間に回線ごと削除されていたら戻さない(外部キー違反で落ちるため)
                if (repository.getLine(action.purchase.lineId) != null) {
                    repository.addPurchase(action.purchase.copy(id = 0))
                }
            }
            is UndoableAction.ArchiveChanged -> repository.setArchived(action.lineId, !action.archived)
        }
    }
}

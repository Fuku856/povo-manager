package com.fuku856.povomanager.data

import com.fuku856.povomanager.data.db.LineDao
import com.fuku856.povomanager.data.db.LineWithPurchases
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 回線・購入履歴の読み書き。書き込み後のウィジェット更新は要求だけして描画の完了を待たない
 * (画面の反応やトーストを描画で遅らせない)。描画完了を待つ必要がある Worker などは
 * [WidgetUpdater.updateAll] を直接呼ぶ。
 */
@Singleton
class LineRepository @Inject constructor(
    private val dao: LineDao,
    private val widgetUpdater: WidgetUpdater,
) {
    /** ホーム表示用。アーカイブ済みは除外 */
    fun observeActiveLinesWithPurchases(): Flow<List<LineWithPurchases>> =
        dao.observeActiveLinesWithPurchases()

    /** アーカイブ済み一覧画面用 */
    fun observeArchivedLinesWithPurchases(): Flow<List<LineWithPurchases>> =
        dao.observeArchivedLinesWithPurchases()

    /** アーカイブ済み件数のみを監視する(行は読み込まない) */
    fun observeArchivedCount(): Flow<Int> = dao.observeArchivedCount()

    fun observeLineWithPurchases(lineId: Long): Flow<LineWithPurchases?> =
        dao.observeLineWithPurchases(lineId)

    suspend fun getLinesWithPurchases(): List<LineWithPurchases> = dao.getLinesWithPurchases()

    /** 通知・ウィジェット用。アーカイブ済みは除外 */
    suspend fun getActiveLinesWithPurchases(): List<LineWithPurchases> =
        dao.getActiveLinesWithPurchases()

    suspend fun getLine(lineId: Long): PovoLine? = dao.getLine(lineId)

    suspend fun addLine(line: PovoLine): Long =
        dao.insertLine(line).also { widgetUpdater.requestUpdate() }

    suspend fun updateLine(line: PovoLine) {
        dao.updateLine(line)
        widgetUpdater.requestUpdate()
    }

    suspend fun deleteLine(line: PovoLine) {
        dao.deleteLine(line)
        widgetUpdater.requestUpdate()
    }

    /**
     * 回線のアーカイブ状態だけを変更する。行全体を書き戻さないため、
     * 同時に走る編集・インポートの内容を古い値で上書きしない。
     * @return 状態が実際に変わったら true(既にその状態・回線が無いときは false)
     */
    suspend fun setArchived(lineId: Long, archived: Boolean): Boolean {
        val changed = dao.updateArchived(lineId, archived) > 0
        if (changed) widgetUpdater.requestUpdate()
        return changed
    }

    /** ウィジェットの手動並び替え順を保存する。リストの並び順を sortOrder として書き込む。 */
    suspend fun setLineOrder(orderedIds: List<Long>) {
        dao.updateLineSortOrders(orderedIds)
        widgetUpdater.requestUpdate()
    }

    suspend fun addPurchase(purchase: ToppingPurchase): Long =
        dao.insertPurchase(purchase).also { widgetUpdater.requestUpdate() }

    suspend fun updatePurchase(purchase: ToppingPurchase) {
        dao.updatePurchase(purchase)
        widgetUpdater.requestUpdate()
    }

    suspend fun deletePurchase(purchase: ToppingPurchase) {
        dao.deletePurchase(purchase)
        widgetUpdater.requestUpdate()
    }

    /** インポート時の全置換。linesと購入履歴はインデックスで対応付け */
    suspend fun replaceAll(data: List<LineWithPurchases>) {
        dao.replaceAll(
            lines = data.map { it.line.copy(id = 0) },
            purchasesByLineIndex = data.withIndex().associate { (index, item) -> index to item.purchases },
        )
        widgetUpdater.requestUpdate()
    }

    /**
     * インポート時のマージ(上書き)。既存データは保持し、電話番号一致で更新・それ以外は追加。
     * @param keepArchivedIndices 既存回線のアーカイブ状態を維持する回線の添字(旧形式のバックアップ由来)
     */
    suspend fun mergeImport(data: List<LineWithPurchases>, keepArchivedIndices: Set<Int> = emptySet()) {
        dao.mergeImport(
            lines = data.map { it.line.copy(id = 0) },
            purchasesByLineIndex = data.withIndex().associate { (index, item) -> index to item.purchases },
            keepArchivedIndices = keepArchivedIndices,
        )
        widgetUpdater.requestUpdate()
    }
}

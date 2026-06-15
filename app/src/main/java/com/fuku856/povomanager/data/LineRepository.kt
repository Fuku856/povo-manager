package com.fuku856.povomanager.data

import com.fuku856.povomanager.data.db.LineDao
import com.fuku856.povomanager.data.db.LineWithPurchases
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LineRepository @Inject constructor(
    private val dao: LineDao,
    private val widgetUpdater: WidgetUpdater,
) {
    fun observeLinesWithPurchases(): Flow<List<LineWithPurchases>> = dao.observeLinesWithPurchases()

    /** ホーム表示用。アーカイブ済みは除外 */
    fun observeActiveLinesWithPurchases(): Flow<List<LineWithPurchases>> =
        dao.observeActiveLinesWithPurchases()

    /** アーカイブ済み一覧画面用 */
    fun observeArchivedLinesWithPurchases(): Flow<List<LineWithPurchases>> =
        dao.observeArchivedLinesWithPurchases()

    fun observeLineWithPurchases(lineId: Long): Flow<LineWithPurchases?> =
        dao.observeLineWithPurchases(lineId)

    suspend fun getLinesWithPurchases(): List<LineWithPurchases> = dao.getLinesWithPurchases()

    /** 通知・ウィジェット用。アーカイブ済みは除外 */
    suspend fun getActiveLinesWithPurchases(): List<LineWithPurchases> =
        dao.getActiveLinesWithPurchases()

    suspend fun getLine(lineId: Long): PovoLine? = dao.getLine(lineId)

    suspend fun addLine(line: PovoLine): Long =
        dao.insertLine(line).also { widgetUpdater.updateAll() }

    suspend fun updateLine(line: PovoLine) {
        dao.updateLine(line)
        widgetUpdater.updateAll()
    }

    suspend fun deleteLine(line: PovoLine) {
        dao.deleteLine(line)
        widgetUpdater.updateAll()
    }

    /** 回線のアーカイブ状態を変更する。updateLine 内でウィジェットも更新される。 */
    suspend fun setArchived(line: PovoLine, archived: Boolean) {
        updateLine(line.copy(isArchived = archived))
    }

    /** ウィジェットの手動並び替え順を保存する。リストの並び順を sortOrder として書き込む。 */
    suspend fun setLineOrder(orderedIds: List<Long>) {
        dao.updateLineSortOrders(orderedIds)
        widgetUpdater.updateAll()
    }

    suspend fun addPurchase(purchase: ToppingPurchase): Long =
        dao.insertPurchase(purchase).also { widgetUpdater.updateAll() }

    suspend fun updatePurchase(purchase: ToppingPurchase) {
        dao.updatePurchase(purchase)
        widgetUpdater.updateAll()
    }

    suspend fun deletePurchase(purchase: ToppingPurchase) {
        dao.deletePurchase(purchase)
        widgetUpdater.updateAll()
    }

    /** インポート時の全置換。linesと購入履歴はインデックスで対応付け */
    suspend fun replaceAll(data: List<LineWithPurchases>) {
        dao.replaceAll(
            lines = data.map { it.line.copy(id = 0) },
            purchasesByLineIndex = data.withIndex().associate { (index, item) -> index to item.purchases },
        )
        widgetUpdater.updateAll()
    }

    /** インポート時のマージ(上書き)。既存データは保持し、電話番号一致で更新・それ以外は追加 */
    suspend fun mergeImport(data: List<LineWithPurchases>) {
        dao.mergeImport(
            lines = data.map { it.line.copy(id = 0) },
            purchasesByLineIndex = data.withIndex().associate { (index, item) -> index to item.purchases },
        )
        widgetUpdater.updateAll()
    }
}

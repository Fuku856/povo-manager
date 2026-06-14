package com.fuku856.povomanager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LineDao {

    @Transaction
    @Query("SELECT * FROM lines ORDER BY sortOrder, id")
    fun observeLinesWithPurchases(): Flow<List<LineWithPurchases>>

    @Transaction
    @Query("SELECT * FROM lines WHERE id = :lineId")
    fun observeLineWithPurchases(lineId: Long): Flow<LineWithPurchases?>

    @Transaction
    @Query("SELECT * FROM lines ORDER BY sortOrder, id")
    suspend fun getLinesWithPurchases(): List<LineWithPurchases>

    @Query("SELECT * FROM lines WHERE id = :lineId")
    suspend fun getLine(lineId: Long): PovoLine?

    @Query("SELECT * FROM lines WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getLineByPhone(phoneNumber: String): PovoLine?

    @Query("SELECT * FROM topping_purchases WHERE lineId = :lineId")
    suspend fun getPurchasesForLine(lineId: Long): List<ToppingPurchase>

    @Insert
    suspend fun insertLine(line: PovoLine): Long

    @Update
    suspend fun updateLine(line: PovoLine)

    @Query("UPDATE lines SET sortOrder = :order WHERE id = :id")
    suspend fun updateLineSortOrder(id: Long, order: Int)

    /** 並び順を一括更新する。中途半端な並びが残らないようトランザクションで囲む。 */
    @Transaction
    suspend fun updateLineSortOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updateLineSortOrder(id, index) }
    }

    @Delete
    suspend fun deleteLine(line: PovoLine)

    @Insert
    suspend fun insertPurchase(purchase: ToppingPurchase): Long

    @Update
    suspend fun updatePurchase(purchase: ToppingPurchase)

    @Delete
    suspend fun deletePurchase(purchase: ToppingPurchase)

    @Query("DELETE FROM lines")
    suspend fun deleteAllLines()

    @Insert
    suspend fun insertLines(lines: List<PovoLine>): List<Long>

    @Insert
    suspend fun insertPurchases(purchases: List<ToppingPurchase>)

    /** インポート時の全置換 */
    @Transaction
    suspend fun replaceAll(lines: List<PovoLine>, purchasesByLineIndex: Map<Int, List<ToppingPurchase>>) {
        deleteAllLines()
        val newIds = insertLines(lines)
        val purchases = purchasesByLineIndex.flatMap { (index, list) ->
            list.map { it.copy(id = 0, lineId = newIds[index]) }
        }
        insertPurchases(purchases)
    }

    /**
     * インポート時のマージ(上書き)。既存データは削除しない。
     * 電話番号が一致する回線は情報を更新し、一致しない回線は新規追加する。
     * 購入履歴は (購入日, トッピング名, 有効期限) が完全一致するものは重複追加しない。
     */
    @Transaction
    suspend fun mergeImport(lines: List<PovoLine>, purchasesByLineIndex: Map<Int, List<ToppingPurchase>>) {
        lines.forEachIndexed { index, line ->
            val existing = getLineByPhone(line.phoneNumber)
            val targetId = if (existing != null) {
                updateLine(line.copy(id = existing.id))
                existing.id
            } else {
                insertLine(line.copy(id = 0))
            }
            val existingPurchases = getPurchasesForLine(targetId)
            val newPurchases = (purchasesByLineIndex[index] ?: emptyList())
                .filterNot { p ->
                    existingPurchases.any {
                        it.purchaseDate == p.purchaseDate &&
                            it.toppingName == p.toppingName &&
                            it.validityEndDate == p.validityEndDate
                    }
                }
                .map { it.copy(id = 0, lineId = targetId) }
            insertPurchases(newPurchases)
        }
    }
}

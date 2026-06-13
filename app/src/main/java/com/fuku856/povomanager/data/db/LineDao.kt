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

    @Insert
    suspend fun insertLine(line: PovoLine): Long

    @Update
    suspend fun updateLine(line: PovoLine)

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
}

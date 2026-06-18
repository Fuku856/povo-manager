package com.fuku856.povomanager.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.fuku856.povomanager.domain.SimType
import java.time.LocalDate

@Entity(tableName = "lines")
data class PovoLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 電話番号(必須) */
    val phoneNumber: String,
    /** 任意の表示名。未設定時は電話番号を表示 */
    val name: String? = null,
    /** 通知タイミングの回線ごと上書き。null=共通設定を使用 */
    val notifyDaysOverride: Set<Int>? = null,
    val memo: String? = null,
    /** SIM種別(物理SIM/eSIM)。この機能より前に登録された回線はnull */
    val simType: SimType? = null,
    val sortOrder: Int = 0,
    /** アーカイブ済みか。trueの間はホーム一覧・ウィジェット・通知から除外される */
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "topping_purchases",
    foreignKeys = [
        ForeignKey(
            entity = PovoLine::class,
            parentColumns = ["id"],
            childColumns = ["lineId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("lineId")],
)
data class ToppingPurchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lineId: Long,
    val purchaseDate: LocalDate,
    val toppingName: String,
    /** トッピング自体の有効期限(最終日)。自動更新型や不明の場合はnull */
    val validityEndDate: LocalDate? = null,
)

data class LineWithPurchases(
    @Embedded val line: PovoLine,
    @Relation(parentColumn = "id", entityColumn = "lineId")
    val purchases: List<ToppingPurchase>,
)

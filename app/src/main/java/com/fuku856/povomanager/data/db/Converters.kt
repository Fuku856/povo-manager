package com.fuku856.povomanager.data.db

import androidx.room.TypeConverter
import com.fuku856.povomanager.domain.SimType
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    /** Set<Int> はカンマ区切り文字列で保存。null(=共通設定)と空集合(=通知なし)を区別する */
    @TypeConverter
    fun intSetToString(set: Set<Int>?): String? = set?.joinToString(",")

    @TypeConverter
    fun stringToIntSet(value: String?): Set<Int>? = when {
        value == null -> null
        value.isEmpty() -> emptySet()
        // 不正・空白トークンが混入しても落ちないよう toIntOrNull で防御する
        else -> value.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    /** SIM種別は enum 名で保存。null=未設定 */
    @TypeConverter
    fun simTypeToString(simType: SimType?): String? = simType?.name

    @TypeConverter
    fun stringToSimType(value: String?): SimType? =
        // 不明な値(将来の追加列や破損データ)で落ちないよう防御する
        value?.let { runCatching { SimType.valueOf(it) }.getOrNull() }
}

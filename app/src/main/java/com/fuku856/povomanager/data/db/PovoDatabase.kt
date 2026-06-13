package com.fuku856.povomanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PovoLine::class, ToppingPurchase::class],
    version = 1,
    // スキーマ履歴を app/schemas に出力する(将来のマイグレーション作成に必須)。
    // 出力先は build.gradle.kts の ksp { arg("room.schemaLocation", ...) } で指定。
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PovoDatabase : RoomDatabase() {
    abstract fun lineDao(): LineDao
}

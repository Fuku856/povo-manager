package com.fuku856.povomanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PovoLine::class, ToppingPurchase::class],
    version = 4,
    // スキーマ履歴を app/schemas に出力する(将来のマイグレーション作成に必須)。
    // 出力先は build.gradle.kts の ksp { arg("room.schemaLocation", ...) } で指定。
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PovoDatabase : RoomDatabase() {
    abstract fun lineDao(): LineDao
}

/** v1→v2: アーカイブ機能のため lines.isArchived 列を追加(既定値0=未アーカイブ) */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lines ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}

/** v2→v3: SIM種別のため lines.simType 列を追加(nullable=既存回線は未設定) */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lines ADD COLUMN simType TEXT")
    }
}

/** v3→v4: 購入日時・時間型トッピングのため topping_purchases に時刻列を追加(nullable=既存の購入は時刻なし) */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE topping_purchases ADD COLUMN purchaseTime INTEGER")
        db.execSQL("ALTER TABLE topping_purchases ADD COLUMN validityEndTime INTEGER")
    }
}

package com.fuku856.povomanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PovoLine::class, ToppingPurchase::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PovoDatabase : RoomDatabase() {
    abstract fun lineDao(): LineDao
}

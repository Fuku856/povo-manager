package com.fuku856.povomanager.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room マイグレーションの検証。エクスポート済みスキーマ(app/schemas)を
 * unit test のアセットに含め、Robolectric 上で実行する(testDebugUnitTest)。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PovoDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsIsArchivedColumnDefaultingToFalse() {
        // v1 スキーマ(isArchived 列なし)でDBを作成し、既存行を1件入れる。
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO lines (phoneNumber, name, notifyDaysOverride, memo, sortOrder) " +
                    "VALUES ('09012345678', 'メイン', NULL, NULL, 0)",
            )
            close()
        }

        // v2 へマイグレーション。第3引数 true でドロップ漏れ等のスキーマ妥当性も検証される。
        val db = helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2)

        // 既存行は追加列 isArchived が既定値 0(未アーカイブ)になる。
        db.query("SELECT isArchived FROM lines WHERE phoneNumber = '09012345678'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate2To3_addsSimTypeColumnDefaultingToNull() {
        // v2 スキーマ(simType 列なし)でDBを作成し、既存行を1件入れる。
        helper.createDatabase(testDb, 2).apply {
            execSQL(
                "INSERT INTO lines (phoneNumber, name, notifyDaysOverride, memo, sortOrder, isArchived) " +
                    "VALUES ('09012345678', 'メイン', NULL, NULL, 0, 0)",
            )
            close()
        }

        // v3 へマイグレーション。
        val db = helper.runMigrationsAndValidate(testDb, 3, true, MIGRATION_2_3)

        // 既存行は追加列 simType が NULL(未設定)になる。
        db.query("SELECT simType FROM lines WHERE phoneNumber = '09012345678'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
    }

    @Test
    fun migrate3To4_addsTimeColumnsDefaultingToNull() {
        // v3 スキーマ(時刻列なし)でDBを作成し、回線と購入履歴を1件ずつ入れる。
        helper.createDatabase(testDb, 3).apply {
            execSQL(
                "INSERT INTO lines (id, phoneNumber, name, notifyDaysOverride, memo, sortOrder, isArchived, simType) " +
                    "VALUES (1, '09012345678', 'メイン', NULL, NULL, 0, 0, 'ESIM')",
            )
            execSQL(
                "INSERT INTO topping_purchases (lineId, purchaseDate, toppingName, validityEndDate) " +
                    "VALUES (1, 20000, 'データ追加1GB(7日間)', 20006)",
            )
            close()
        }

        // v4 へマイグレーション。
        val db = helper.runMigrationsAndValidate(testDb, 4, true, MIGRATION_3_4)

        // 既存の購入は日付を保ったまま、追加列 purchaseTime / validityEndTime が NULL(時刻なし)になる。
        db.query("SELECT purchaseDate, validityEndDate, purchaseTime, validityEndTime FROM topping_purchases").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(20000L, cursor.getLong(0))
            assertEquals(20006L, cursor.getLong(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
    }
}

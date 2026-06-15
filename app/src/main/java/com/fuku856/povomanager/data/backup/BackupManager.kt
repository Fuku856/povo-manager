package com.fuku856.povomanager.data.backup

import android.content.Context
import android.net.Uri
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.db.LineWithPurchases
import com.fuku856.povomanager.data.db.PovoLine
import com.fuku856.povomanager.data.db.ToppingPurchase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** インポート方法 */
enum class ImportMode {
    /** 既存データをすべて削除してファイルの内容に置き換える */
    REPLACE,

    /** 既存データを保持し、電話番号一致で上書き・それ以外は追加する */
    MERGE,
}

/** インポート前プレビュー用の回線概要(電話番号・回線名) */
data class ImportPreviewLine(val phoneNumber: String, val name: String?)

@Serializable
data class BackupData(
    val version: Int = BackupManager.CURRENT_VERSION,
    val exportedAt: String,
    val lines: List<BackupLine>,
)

@Serializable
data class BackupLine(
    val phoneNumber: String,
    val name: String? = null,
    val memo: String? = null,
    val notifyDaysOverride: List<Int>? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val purchases: List<BackupPurchase> = emptyList(),
)

@Serializable
data class BackupPurchase(
    val purchaseDate: String,
    val toppingName: String,
    val validityEndDate: String? = null,
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LineRepository,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    companion object {
        /** 現在のバックアップ形式バージョン。これより新しいファイルは取り込まない */
        const val CURRENT_VERSION = 1
    }

    /** 全データをJSONとして書き出す。返り値は回線数 */
    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val data = repository.getLinesWithPurchases()
        val backup = BackupData(
            exportedAt = LocalDate.now().toString(),
            lines = data.map { it.toBackup() },
        )
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(json.encodeToString(BackupData.serializer(), backup).toByteArray(Charsets.UTF_8))
        } ?: error("ファイルを開けませんでした")
        backup.lines.size
    }

    /**
     * ファイルを読み込み、取り込まれる回線の一覧(電話番号・回線名)を返す。
     * インポートは行わないため、確定前のプレビュー表示に使う。
     */
    suspend fun preview(uri: Uri): List<ImportPreviewLine> = withContext(Dispatchers.IO) {
        readBackup(uri).lines.map { ImportPreviewLine(it.phoneNumber, it.name) }
    }

    /**
     * JSONを読み込み、指定モードでインポートする。返り値は取り込んだ回線数。
     * @param mode REPLACE=全置換 / MERGE=上書き(既存保持)
     */
    suspend fun importFrom(uri: Uri, mode: ImportMode): Int = withContext(Dispatchers.IO) {
        val backup = readBackup(uri)
        // 日付パースは replaceAll/mergeImport の前に行われるため、不正な日付があれば
        // DB を変更せずにここで例外となる(既存データは破壊されない)
        val entities = backup.lines.map { it.toEntity() }
        when (mode) {
            ImportMode.REPLACE -> repository.replaceAll(entities)
            ImportMode.MERGE -> repository.mergeImport(entities)
        }
        backup.lines.size
    }

    /** ファイルを読み込み、形式バージョンを検証して [BackupData] を返す。 */
    private fun readBackup(uri: Uri): BackupData {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error("ファイルを開けませんでした")
        val backup = json.decodeFromString(BackupData.serializer(), text)
        // 未対応の新しい形式を黙って取り込まない
        require(backup.version <= CURRENT_VERSION) {
            "対応していないバックアップ形式です(version=${backup.version})"
        }
        return backup
    }

    private fun LineWithPurchases.toBackup() = BackupLine(
        phoneNumber = line.phoneNumber,
        name = line.name,
        memo = line.memo,
        notifyDaysOverride = line.notifyDaysOverride?.toList(),
        sortOrder = line.sortOrder,
        isArchived = line.isArchived,
        purchases = purchases.map {
            BackupPurchase(
                purchaseDate = it.purchaseDate.toString(),
                toppingName = it.toppingName,
                validityEndDate = it.validityEndDate?.toString(),
            )
        },
    )

    private fun BackupLine.toEntity() = LineWithPurchases(
        line = PovoLine(
            phoneNumber = phoneNumber,
            name = name,
            memo = memo,
            notifyDaysOverride = notifyDaysOverride?.toSet(),
            sortOrder = sortOrder,
            isArchived = isArchived,
        ),
        purchases = purchases.map {
            ToppingPurchase(
                lineId = 0,
                purchaseDate = LocalDate.parse(it.purchaseDate),
                toppingName = it.toppingName,
                validityEndDate = it.validityEndDate?.let(LocalDate::parse),
            )
        },
    )
}

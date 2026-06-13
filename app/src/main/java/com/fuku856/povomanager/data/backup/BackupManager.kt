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

@Serializable
data class BackupData(
    val version: Int = 1,
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

    /** JSONを読み込み、既存データをすべて置き換える。返り値は回線数 */
    suspend fun importFrom(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error("ファイルを開けませんでした")
        val backup = json.decodeFromString(BackupData.serializer(), text)
        repository.replaceAll(backup.lines.map { it.toEntity() })
        backup.lines.size
    }

    private fun LineWithPurchases.toBackup() = BackupLine(
        phoneNumber = line.phoneNumber,
        name = line.name,
        memo = line.memo,
        notifyDaysOverride = line.notifyDaysOverride?.toList(),
        sortOrder = line.sortOrder,
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

package com.fuku856.povomanager.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.domain.TOPPING_PRESETS
import com.fuku856.povomanager.domain.ValidityEnd
import com.fuku856.povomanager.domain.validityEnd
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * トッピング購入の登録/編集シート。
 * プリセット選択で名前と有効期限を自動入力、自由入力も可能。
 *
 * @param initial 編集対象の購入。null なら新規記録で、「アプリに追加した時刻を購入日時にする」
 *   チェック(既定ON)を表示する。編集時は既存の購入日時を直接修正する。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PurchaseSheet(
    title: String,
    initial: ToppingPurchase? = null,
    onConfirm: (purchasedAt: LocalDateTime, toppingName: String, validityEnd: ValidityEnd?) -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = initial == null
    var useAddedTime by rememberSaveable { mutableStateOf(isNew) }
    var date by rememberSaveable { mutableStateOf(initial?.purchaseDate ?: LocalDate.now()) }
    var time by rememberSaveable { mutableStateOf<LocalTime?>(initial?.purchaseTime) }
    var name by rememberSaveable { mutableStateOf(initial?.toppingName.orEmpty()) }
    // 編集時は名前が一致するプリセットを選択済みにし、購入日時を直したら有効期限も計算し直す
    var selectedPresetName by rememberSaveable {
        mutableStateOf(TOPPING_PRESETS.find { it.name == initial?.toppingName }?.name)
    }
    val selectedPreset = TOPPING_PRESETS.find { it.name == selectedPresetName }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Text("購入日時", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            if (isNew) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useAddedTime, onCheckedChange = { useAddedTime = it })
                    Text("アプリに追加した時刻を購入日時にする", style = MaterialTheme.typography.bodyMedium)
                }
            }
            PurchaseDateTimeField(
                date = date,
                time = time,
                onDateChange = { date = it },
                onTimeChange = { time = it },
                enabled = !useAddedTime,
                isError = time == null,
            )

            Spacer(Modifier.height(16.dp))
            Text("トッピング", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TOPPING_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = {
                            selectedPresetName = if (selectedPreset == preset) null else preset.name
                            if (selectedPresetName == preset.name) name = preset.name
                        },
                        label = { Text(preset.name) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (selectedPreset?.name != it) selectedPresetName = null
                },
                label = { Text("トッピング名(自由入力)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("キャンセル") }
                Spacer(Modifier.width(8.dp))
                Button(
                    // 購入時刻は必須(「アプリに追加した時刻」を使う場合は記録時点の時刻)
                    enabled = name.isNotBlank() && (useAddedTime || time != null),
                    onClick = {
                        val purchasedAt = if (useAddedTime) {
                            LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                        } else {
                            date.atTime(time ?: return@Button)
                        }
                        // プリセット選択時は購入日時から有効期限を計算(povoの規定に従う)、
                        // 未選択時は編集前の有効期限を維持
                        val validityEnd = selectedPreset?.validityEnd(purchasedAt)
                            ?: initial?.validityEndDate
                                ?.takeIf { selectedPreset == null }
                                ?.let { ValidityEnd(it, initial?.validityEndTime) }
                        onConfirm(purchasedAt, name.trim(), validityEnd)
                    },
                ) { Text("記録する") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

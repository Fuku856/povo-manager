package com.fuku856.povomanager.ui.settings

import android.content.res.Configuration
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuku856.povomanager.data.backup.ImportMode
import com.fuku856.povomanager.data.settings.AppSettings
import com.fuku856.povomanager.ui.common.formatPhoneNumber
import com.fuku856.povomanager.ui.lineedit.NotifyDayChips
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportTo) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { pendingImportUri = it } }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionTitle("通知")

            Column {
                Text("自動解約日の通知タイミング(共通設定)", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "回線ごとの個別設定がある場合はそちらが優先されます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                NotifyDayChips(
                    selected = settings.defaultNotifyDays,
                    onToggle = viewModel::toggleNotifyDay,
                )
            }

            Column {
                Text("トッピング有効期限の通知タイミング", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                NotifyDayChips(
                    selected = settings.toppingExpiryNotifyDays,
                    onToggle = viewModel::toggleToppingNotifyDay,
                    choices = AppSettings.TOPPING_NOTIFY_DAY_CHOICES,
                )
            }

            NotifyTimeSelector(
                hour = settings.notifyHour,
                minute = settings.notifyMinute,
                onTimeChange = viewModel::setNotifyTime,
            )

            HorizontalDivider()
            SectionTitle("詳細設定")

            ExpiryPeriodField(
                value = settings.expiryPeriodDays,
                onCommit = viewModel::setExpiryPeriodDays,
            )

            HorizontalDivider()
            SectionTitle("バックアップ")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "全回線と購入履歴をJSONファイルに保存/復元できます。機種変更時のデータ移行にご利用ください。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("povo-manager-backup.json") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("エクスポート")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("インポート")
                    }
                }
            }
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("インポート方法を選択") },
            text = {
                Text(
                    "追加: 既存の回線は残し、同じ電話番号は更新・新しい回線は追加します。\n" +
                        "全置換: 現在の回線・購入履歴をすべて削除し、ファイルの内容に置き換えます。",
                )
            },
            // 上段に「追加」「全置換」、下段に「キャンセル」を表示する。
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            viewModel.prepareImport(uri, ImportMode.MERGE)
                            pendingImportUri = null
                        }) { Text("追加") }
                        TextButton(onClick = {
                            viewModel.prepareImport(uri, ImportMode.REPLACE)
                            pendingImportUri = null
                        }) { Text("全置換", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { pendingImportUri = null }) { Text("キャンセル") }
                }
            },
        )
    }

    importPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            onConfirm = viewModel::confirmImport,
            onCancel = viewModel::cancelImport,
        )
    }
}

@Composable
private fun ImportPreviewDialog(
    preview: ImportPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val isReplace = preview.mode == ImportMode.REPLACE
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (isReplace) "全置換の確認" else "追加の確認") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (isReplace) {
                        "現在の回線をすべて削除し、以下の${preview.lines.size}回線に置き換えます。"
                    } else {
                        "以下の${preview.lines.size}回線を取り込みます(同じ電話番号は更新)。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                if (preview.lines.isEmpty()) {
                    Text(
                        if (isReplace) {
                            "⚠ ファイルに回線が含まれていません。このまま実行すると既存の回線がすべて削除されます。"
                        } else {
                            "⚠ ファイルに回線が含まれていません。取り込まれる回線はありません。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(preview.lines) { line ->
                            val name = line.name?.takeIf { it.isNotBlank() }
                            Column {
                                Text(
                                    name ?: formatPhoneNumber(line.phoneNumber),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (name != null) {
                                    Text(
                                        formatPhoneNumber(line.phoneNumber),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = preview.lines.isNotEmpty() || isReplace,
            ) {
                Text(
                    if (isReplace) "全置換する" else "追加する",
                    color = if (isReplace) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("キャンセル") }
        },
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifyTimeSelector(hour: Int, minute: Int, onTimeChange: (Int, Int) -> Unit) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Column {
        Text("通知時刻", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("%02d:%02d".format(hour, minute))
        }
    }

    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        // 針を動かして時/分が変わるたびに、OS標準の時計ピッカーと同じ「カチッ」という
        // 触覚フィードバックを鳴らす。初回の値はスキップする。
        val view = LocalView.current
        LaunchedEffect(timePickerState) {
            snapshotFlow { timePickerState.hour to timePickerState.minute }
                .drop(1)
                .collect {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(timePickerState.hour, timePickerState.minute)
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("キャンセル") }
            },
            title = { Text("通知時刻") },
            text = {
                // 横画面では時計ダイヤルが収まらずボタンが押せなくなるため、
                // 縦画面はダイヤル(TimePicker)、横画面は数値入力(TimeInput)に切り替える。
                // どちらでも高さが不足したときに見切れないよう縦スクロールを許可する。
                val isPortrait = LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isPortrait) {
                        TimePicker(state = timePickerState)
                    } else {
                        TimeInput(state = timePickerState)
                    }
                }
            },
        )
    }
}

@Composable
private fun ExpiryPeriodField(value: Int, onCommit: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { text = value.toString() }
    val focusManager = LocalFocusManager.current

    // 入力途中の中間値を保存しないよう、フォーカス喪失/Done時にのみ確定する
    fun commit() {
        val parsed = text.toIntOrNull()?.coerceIn(1, 3650)
        if (parsed != null) {
            if (parsed != value) onCommit(parsed)
            text = parsed.toString()
        } else {
            text = value.toString() // 空など無効な入力は元に戻す
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input -> text = input.filter(Char::isDigit).take(4) },
        label = { Text("解約までの日数") },
        supportingText = { Text("povoの規約変更があった場合に調整できます(通常は180)") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = {
            commit()
            focusManager.clearFocus()
        }),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused) commit() },
    )
}

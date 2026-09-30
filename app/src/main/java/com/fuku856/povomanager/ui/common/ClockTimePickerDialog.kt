package com.fuku856.povomanager.ui.common

import android.content.res.Configuration
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.drop

/**
 * OS標準の時計と同じアナログダイヤルで時刻を選ぶダイアログ(24時間表記)。
 * 通知時刻の設定と購入日時の入力で共用する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    // 針を動かして時/分が変わるたびに、OS標準の時計ピッカーと同じ「カチッ」という
    // 触覚フィードバックを鳴らす。初回の値はスキップする。
    val view = LocalView.current
    // 指を離して確定したときは、ティックより少し強い確定用の振動を鳴らす。
    // CONFIRM は API 30 以降のため、それ未満では LONG_PRESS で代替する。
    val confirmHaptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.LONG_PRESS
    }
    LaunchedEffect(timePickerState) {
        snapshotFlow { timePickerState.hour to timePickerState.minute }
            .drop(1)
            .collect {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
        title = { Text(title) },
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
                    TimePicker(
                        state = timePickerState,
                        // ダイヤル上で指が離れた(=その値で確定した)瞬間に確定用の振動を鳴らす。
                        // requireUnconsumed=false と未consumeでイベントを観測のみ行い、
                        // TimePicker本来の操作は阻害しない。
                        modifier = Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                                view.performHapticFeedback(confirmHaptic)
                            }
                        },
                    )
                } else {
                    TimeInput(state = timePickerState)
                }
            }
        },
    )
}

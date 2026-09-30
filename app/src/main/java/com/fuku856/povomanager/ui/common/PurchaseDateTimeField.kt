package com.fuku856.povomanager.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * 購入日時の入力欄。日付(カレンダー)と時刻(アナログ時計)のボタンを横に並べる。
 * 時刻は必須で、未選択なら「時刻を選択」と表示する。日付を選んだとき時刻が未選択なら、
 * 続けて時計ダイアログを開いて日時をひと続きで入力できるようにする。
 *
 * @param enabled false のときはグレーアウトして操作できない
 * @param isError true のとき時刻ボタンをエラー色にし、未選択である旨を下に表示する
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDateTimeField(
    date: LocalDate,
    time: LocalTime?,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val showError = enabled && isError

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }, enabled = enabled) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(date.toDisplayString())
            }
            OutlinedButton(
                onClick = { showTimePicker = true },
                enabled = enabled,
                border = if (showError) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.outlinedButtonBorder(enabled)
                },
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    time?.toDisplayString() ?: "時刻を選択",
                    color = if (showError) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        }
        if (showError) {
            Spacer(Modifier.height(4.dp))
            Text(
                "購入時刻を選択してください",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                    if (time == null) showTimePicker = true
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initial = time ?: LocalTime.now()
        ClockTimePickerDialog(
            title = "購入時刻",
            initialHour = initial.hour,
            initialMinute = initial.minute,
            onConfirm = { hour, minute ->
                onTimeChange(LocalTime.of(hour, minute))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

package com.fuku856.povomanager.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fuku856.povomanager.domain.SimType

/**
 * 回線のSIM種別を表す小さなチップ。一覧/詳細/アーカイブで見た目を統一するため共通化する。
 * [simType] が null(未設定の旧データ)の場合は何も描画しない。
 */
@Composable
fun SimTypeChip(simType: SimType?, modifier: Modifier = Modifier) {
    if (simType == null) return
    SuggestionChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = { Text(simType.label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

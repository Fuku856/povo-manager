package com.fuku856.povomanager.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fuku856.povomanager.domain.SimType

/**
 * 回線のSIM種別を表す小さなラベル。一覧/詳細/アーカイブで見た目を統一するため共通化する。
 * 表示専用なので Chip(操作要素・最小タップ領域 48dp を確保する)ではなく枠付きテキストで描く。
 * [simType] が null(未設定の旧データ)の場合は何も描画しない。
 */
@Composable
fun SimTypeChip(simType: SimType?, modifier: Modifier = Modifier) {
    if (simType == null) return
    Text(
        text = simType.label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

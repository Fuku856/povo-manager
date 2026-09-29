package com.fuku856.povomanager.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fuku856.povomanager.data.db.PovoLine

/**
 * 回線カードの見出し。回線名(長い場合は省略)+SIM種別、回線名がある場合は電話番号を併記する。
 * ホームとアーカイブ一覧のカードで共用する。
 */
@Composable
fun LineHeader(line: PovoLine, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                line.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // SIM種別を押し出さないよう、名前側だけを縮める
                modifier = Modifier.weight(1f, fill = false),
            )
            line.simType?.let {
                Spacer(Modifier.width(8.dp))
                SimTypeChip(it)
            }
        }
        if (!line.name.isNullOrBlank()) {
            Text(
                formatPhoneNumber(line.phoneNumber),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

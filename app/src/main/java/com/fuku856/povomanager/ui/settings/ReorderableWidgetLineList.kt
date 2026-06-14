package com.fuku856.povomanager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

private val ROW_HEIGHT = 56.dp
private val ROW_SPACING = 8.dp

/**
 * ドラッグ&ドロップで並び替えできる回線リスト(ウィジェット表示順の手動設定用)。
 *
 * 設定画面は verticalScroll の単一 Column のため LazyColumn は使わず、通常 Column と
 * 固定行高で実装する。回線数は少数のため十分軽量。並び替え確定時に [onOrderChanged] を呼ぶ。
 */
@Composable
fun ReorderableWidgetLineList(
    lines: List<WidgetLineRow>,
    onOrderChanged: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ドラッグ中はローカル状態で並びを操作し、確定時に親へ通知する。
    val items = remember { mutableStateListOf<WidgetLineRow>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // ドラッグ中でないときだけ外部の最新リストに同期する(自分の更新と競合させない)。
    LaunchedEffect(lines, draggingId) {
        if (draggingId == null && items.map { it.id } != lines.map { it.id }) {
            items.clear()
            items.addAll(lines)
        }
    }

    val rowStridePx = with(LocalDensity.current) { (ROW_HEIGHT + ROW_SPACING).toPx() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        items.forEach { item ->
            // 並び替えでノード(実行中のドラッグジェスチャ)が破棄されないよう id でキー付けする。
            key(item.id) {
                val isDragging = item.id == draggingId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                            shadowElevation = if (isDragging) 8f else 0f
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "長押ししてドラッグで並び替え",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(28.dp)
                            // 長押し起動にして、親の縦スクロールにドラッグを奪われないようにする。
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = item.id
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        onOrderChanged(items.map { it.id })
                                        draggingId = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val currentIndex = items.indexOfFirst { it.id == draggingId }
                                        if (currentIndex < 0) return@detectDragGesturesAfterLongPress
                                        val shift = (dragOffsetY / rowStridePx).roundToInt()
                                        val target = (currentIndex + shift).coerceIn(0, items.lastIndex)
                                        if (target != currentIndex) {
                                            items.add(target, items.removeAt(currentIndex))
                                            // 入れ替えた分だけ基準をずらし、残りのオフセットを保持する。
                                            dragOffsetY -= (target - currentIndex) * rowStridePx
                                        }
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

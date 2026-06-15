package com.fuku856.povomanager.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** アーカイブ系UIで共通利用する緑。スワイプアクション背景・ボタンアイコンに使う。 */
val ArchiveGreen = Color(0xFF2E7D32)

/**
 * 左スワイプで右端に緑のアーカイブアクションを露出させ、タップで確定するコンテナ。
 * 反対方向(右)へ戻すと閉じる。確定はアクションのタップのみ(スワイプしきり=自動確定はしない)。
 *
 * スワイプ感度はすべて [SwipeTuning] の定数で調整する。
 */
@Composable
fun SwipeToArchiveBox(
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { SwipeTuning.ActionWidth.toPx() }
    val flingThresholdPx = with(density) { SwipeTuning.FlingVelocityThreshold.toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val shape = RoundedCornerShape(12.dp)

    fun settle(open: Boolean) {
        scope.launch { offsetX.animateTo(if (open) -actionWidthPx else 0f, tween(SwipeTuning.SettleDurationMs)) }
    }

    Box(modifier = modifier.fillMaxWidth().clip(shape)) {
        // 背景: 右端に固定された緑のアーカイブアクション
        Box(modifier = Modifier.matchParentSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(SwipeTuning.ActionWidth)
                    .background(ArchiveGreen)
                    // 半分以上開いているときだけタップで確定(誤タップ防止)
                    .clickable(enabled = offsetX.value <= -actionWidthPx * 0.5f) {
                        onArchive()
                        settle(open = false)
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Archive, contentDescription = "アーカイブ", tint = Color.White)
                Spacer(Modifier.height(2.dp))
                Text(
                    "アーカイブ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }

        // 前面: ドラッグで左へずれるコンテンツ
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(actionWidthPx, flingThresholdPx) {
                    val velocityTracker = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { velocityTracker.resetTracking() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val target = (offsetX.value + dragAmount).coerceIn(-actionWidthPx, 0f)
                            scope.launch { offsetX.snapTo(target) }
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().x
                            val open = if (abs(velocity) > flingThresholdPx) {
                                velocity < 0 // 左向きフリックで開く
                            } else {
                                offsetX.value <= -actionWidthPx * SwipeTuning.OpenThresholdFraction
                            }
                            settle(open)
                        },
                        onDragCancel = { settle(open = false) },
                    )
                },
        ) {
            content()
            // 開いている間だけ本体上にオーバーレイを置き、タップを「閉じる」に割り当てる。
            // 閉じているときは存在しないため、カード本来のタップ(詳細遷移)はそのまま機能する。
            if (offsetX.value < -1f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures { settle(open = false) }
                        },
                )
            }
        }
    }
}

/** スワイプ操作感のチューニング値(1箇所に集約。実機で微調整する前提)。 */
private object SwipeTuning {
    /** 露出するアクションの幅 */
    val ActionWidth = 88.dp
    /** 指を離したとき開く判定: アクション幅に対するドラッグ割合 */
    const val OpenThresholdFraction = 0.4f
    /** これを超える速度の左フリックは即「開く」(dp/s 相当で指定) */
    val FlingVelocityThreshold = 400.dp
    /** 開閉アニメーションの時間。画面遷移(280ms)より気持ち速く */
    const val SettleDurationMs = 220
}

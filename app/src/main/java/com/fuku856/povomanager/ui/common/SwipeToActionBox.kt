package com.fuku856.povomanager.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/** アーカイブ系UIで共通利用する緑。スワイプアクション背景・ボタンアイコンに使う。 */
val ArchiveGreen = Color(0xFF2E7D32)

/**
 * 左スワイプで右端に緑のアクションを露出させ、タップで確定するコンテナ。
 * 反対方向(右)へ戻すと閉じる。確定はアクションのタップのみ(スワイプしきり=自動確定はしない)。
 *
 * アクションのアイコン・ラベルは引数で差し替えられる(アーカイブ/解除など)。
 * スワイプ感度はすべて [SwipeTuning] の定数で調整する。
 *
 * @param scrollInProgress 親リストがスクロール中かを返すラムダ。スクロール開始で開いている
 *   アクションを自動で閉じる。値の読み取りを遅延させ、スクロール中に本コンポーザブルを
 *   再コンポーズさせないためラムダで受ける。
 */
@Composable
fun SwipeToActionBox(
    onAction: () -> Unit,
    actionIcon: ImageVector,
    actionLabel: String,
    modifier: Modifier = Modifier,
    scrollInProgress: () -> Boolean = { false },
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { SwipeTuning.ActionWidth.toPx() }
    val flingThresholdPx = with(density) { SwipeTuning.FlingVelocityThreshold.toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val shape = RoundedCornerShape(12.dp)

    // 閉じ側(右)だけ 0 を上限に固定する。フリックで閉じると settle の spring が指の勢いを
    // 引き継いで 0 を一瞬オーバーシュートし、カードが右へずれて左端に背景の緑が覗くのを防ぐ。
    // 閉じ位置=境界なのでそこで止まって正しい。
    //
    // 開き側に下限は設けない。開き位置(-ActionWidth)は境界ではないため、下限を入れると
    // 勢いよく開いたとき spring が下限に達した時点で animateTo が停止し、-ActionWidth まで
    // 戻らず行き過ぎた位置で止まってしまう。ドラッグ中の伸びは rubberBand が制限済み。
    LaunchedEffect(Unit) {
        offsetX.updateBounds(upperBound = 0f)
    }

    // offsetX.value をコンポジションで直接読むと毎フレーム再コンポーズされるため、
    // 状態が反転したときだけ通知される derived state を経由する。
    val canConfirm by remember(actionWidthPx) {
        derivedStateOf { offsetX.value <= -actionWidthPx * SwipeTuning.ConfirmThresholdFraction }
    }
    val isOpen by remember { derivedStateOf { offsetX.value < -1f } }

    fun settle(open: Boolean, initialVelocity: Float = 0f) {
        scope.launch {
            offsetX.animateTo(
                targetValue = if (open) -actionWidthPx else 0f,
                animationSpec = spring(
                    dampingRatio = SwipeTuning.SettleDampingRatio,
                    stiffness = SwipeTuning.SettleStiffness,
                ),
                initialVelocity = initialVelocity,
            )
        }
    }

    // 開いた状態でページをスクロールしたら閉じる。snapshotFlow でスクロール状態の変化だけを
    // 監視するため、スクロール中に本コンポーザブルは再コンポーズされない。
    LaunchedEffect(Unit) {
        snapshotFlow(scrollInProgress).collect { scrolling ->
            if (scrolling) settle(open = false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            // スワイプの非ジェスチャ代替。TalkBack 等にカスタムアクションとして露出する。
            // mergeDescendants=true で内側のカードと1つのフォーカス要素にまとめ、
            // カードにフォーカスした状態でカスタムアクションが読み上げ・実行できるようにする。
            .semantics(mergeDescendants = true) {
                customActions = listOf(
                    CustomAccessibilityAction(actionLabel) { onAction(); true },
                )
            },
    ) {
        // 背景: 全面を緑で塗る。外側の clip(shape) が角を丸めるので、
        // カードの丸角の隙間(露出部)まで緑がきっちり入る。
        // アイコン/ラベルと確定タップは右端 88dp の Column に配置する。
        Box(modifier = Modifier.matchParentSize().background(ArchiveGreen)) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(SwipeTuning.ActionWidth)
                    // 半分以上開いているときだけタップで確定(誤タップ防止)
                    .clickable(enabled = canConfirm) {
                        onAction()
                        settle(open = false)
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(actionIcon, contentDescription = actionLabel, tint = Color.White)
                Spacer(Modifier.height(2.dp))
                Text(
                    actionLabel,
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
                    // 端を超えた分にラバーバンド抵抗をかけるため、生のドラッグ累積を別途追う。
                    var rawOffset = 0f
                    // snapTo はジェスチャと同じディスパッチャ上で動かす。composition の
                    // rememberCoroutineScope に毎フレーム launch するとフレーム同期がずれて
                    // 追従が一拍遅れるため、pointerInput 自身の coroutineScope から起動する。
                    coroutineScope {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                                rawOffset = offsetX.value
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                rawOffset += dragAmount
                                val target = rubberBand(rawOffset, -actionWidthPx, 0f)
                                launch { offsetX.snapTo(target) }
                            },
                            onDragEnd = {
                                val velocity = velocityTracker.calculateVelocity().x
                                val open = if (abs(velocity) > flingThresholdPx) {
                                    velocity < 0 // 左向きフリックで開く
                                } else {
                                    offsetX.value <= -actionWidthPx * SwipeTuning.OpenThresholdFraction
                                }
                                // 離した瞬間の速度を引き継ぎ、指の勢いのまま開閉する。
                                settle(open, velocity)
                            },
                            onDragCancel = { settle(open = false) },
                        )
                    }
                },
        ) {
            content()
            // 開いている間だけ本体上にオーバーレイを置き、タップを「閉じる」に割り当てる。
            // 閉じているときは存在しないため、カード本来のタップ(詳細遷移)はそのまま機能する。
            //
            // ここで down を消費しないのが要点。横ドラッグ(=スワイプで閉じる)は親の drag
            // ジェスチャに任せたいので、タップか否かが確定するまでイベントを奪わない。
            // detectTapGestures は down を消費してしまい、上を通る閉じドラッグと競合して
            // 戻すスライドが引っかかるため使わない。
            if (isOpen) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                // 指を離すまで待つ。途中で親のドラッグが横移動を消費すると
                                // null が返る(タップではなくスワイプだった)ので何もしない。
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume() // カード本来のタップ(詳細遷移)を抑止して閉じる
                                    settle(open = false)
                                }
                            }
                        },
                )
            }
        }
    }
}

/**
 * 左スワイプで緑の「アーカイブ」アクションを露出させる [SwipeToActionBox] のショートカット。
 */
@Composable
fun SwipeToArchiveBox(
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
    scrollInProgress: () -> Boolean = { false },
    content: @Composable () -> Unit,
) {
    SwipeToActionBox(
        onAction = onArchive,
        actionIcon = Icons.Filled.Archive,
        actionLabel = "アーカイブ",
        modifier = modifier,
        scrollInProgress = scrollInProgress,
        content = content,
    )
}

/**
 * ドラッグ量にゴムのような追従感を出す。範囲内 [min, max] はそのまま返す。
 *
 * - 閉じ側(max を超える=右)はクランプする。背景の緑は右端にしか無いため、ここを伸ばすと
 *   カードが右へずれて左端に緑が露出してしまうため。
 * - 開き側(min を下回る=左)だけラバーバンド。超過分を圧縮しつつ、上限
 *   [SwipeTuning.MaxOverscrollFraction](範囲幅に対する割合)へ漸近させて頭打ちにする。
 *   壁にぶつかる感触を出さないため線形クランプではなく指数で滑らかに飽和させる。
 */
private fun rubberBand(raw: Float, min: Float, max: Float): Float = when {
    raw > max -> max
    raw < min -> {
        val overflow = min - raw
        val limit = (max - min) * SwipeTuning.MaxOverscrollFraction
        min - limit * (1f - exp(-SwipeTuning.OverscrollResistance * overflow / limit))
    }
    else -> raw
}

/** スワイプ操作感のチューニング値(1箇所に集約。実機で微調整する前提)。 */
private object SwipeTuning {
    /** 露出するアクションの幅 */
    val ActionWidth = 88.dp
    /** 指を離したとき開く判定: アクション幅に対するドラッグ割合 */
    const val OpenThresholdFraction = 0.4f
    /**
     * アクションのタップ確定を有効にする最小開き量: アクション幅に対する割合。
     * [OpenThresholdFraction] より大きくして、開ききる前の半端な状態での誤確定を防ぐ。
     */
    const val ConfirmThresholdFraction = 0.5f
    /** これを超える速度の左フリックは即「開く」(dp/s 相当で指定) */
    val FlingVelocityThreshold = 400.dp
    /** 開閉アニメーション(なめらか・跳ねない spring)。下げるほど緩やかに動く。 */
    const val SettleStiffness = Spring.StiffnessMedium
    const val SettleDampingRatio = Spring.DampingRatioNoBouncy
    /** 端を超えてドラッグしたとき、超過分の最初の追従率(0=固定, 1=等倍)。上げるほどよく伸びる。 */
    const val OverscrollResistance = 0.35f
    /** ラバーバンドで伸びる上限。露出幅(ActionWidth)に対する割合で、ここへ漸近して頭打ち。 */
    const val MaxOverscrollFraction = 0.25f
}

package com.fuku856.povomanager.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

enum class Urgency { SAFE, WARNING, ALERT, DANGER, EXPIRED, NONE }

fun urgencyOf(daysRemaining: Long?): Urgency = when {
    daysRemaining == null -> Urgency.NONE
    daysRemaining < 0 -> Urgency.EXPIRED
    daysRemaining <= 7 -> Urgency.DANGER
    daysRemaining <= 14 -> Urgency.ALERT
    daysRemaining <= 30 -> Urgency.WARNING
    else -> Urgency.SAFE
}

/**
 * 残日数バンドの配色パレット(ライト/ダークの container・content ペア)。
 * アプリ本体(Compose)とウィジェット(Glance)で同じ値を参照し、配色のズレを防ぐ。
 * 赤系(DANGER/EXPIRED)はテーマ依存(Material You)のためここには含めない。
 */
object UrgencyPalette {
    // SAFE(緑)
    val SafeContainerLight = Color(0xFFD7F2DC)
    val SafeContentLight = Color(0xFF1B5E2A)
    val SafeContainerDark = Color(0xFF1B3A22)
    val SafeContentDark = Color(0xFF8BD99B)

    // WARNING(黄)
    val WarningContainerLight = Color(0xFFFBEFC9)
    val WarningContentLight = Color(0xFF6D5605)
    val WarningContainerDark = Color(0xFF453A14)
    val WarningContentDark = Color(0xFFF0CE63)

    // ALERT(橙)
    val AlertContainerLight = Color(0xFFFCE3C8)
    val AlertContentLight = Color(0xFF8A4B00)
    val AlertContainerDark = Color(0xFF4A3115)
    val AlertContentDark = Color(0xFFF2B26B)
}

data class UrgencyColors(val container: Color, val content: Color)

@Composable
fun urgencyColors(urgency: Urgency): UrgencyColors {
    val dark = isSystemInDarkTheme()
    return when (urgency) {
        Urgency.SAFE ->
            if (dark) UrgencyColors(UrgencyPalette.SafeContainerDark, UrgencyPalette.SafeContentDark)
            else UrgencyColors(UrgencyPalette.SafeContainerLight, UrgencyPalette.SafeContentLight)
        Urgency.WARNING ->
            if (dark) UrgencyColors(UrgencyPalette.WarningContainerDark, UrgencyPalette.WarningContentDark)
            else UrgencyColors(UrgencyPalette.WarningContainerLight, UrgencyPalette.WarningContentLight)
        Urgency.ALERT ->
            if (dark) UrgencyColors(UrgencyPalette.AlertContainerDark, UrgencyPalette.AlertContentDark)
            else UrgencyColors(UrgencyPalette.AlertContainerLight, UrgencyPalette.AlertContentLight)
        Urgency.DANGER, Urgency.EXPIRED ->
            UrgencyColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        Urgency.NONE ->
            UrgencyColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 残日数バッジ(例: 「あと19日」「期限切れ」「履歴なし」) */
@Composable
fun RemainingDaysBadge(daysRemaining: Long?, modifier: Modifier = Modifier) {
    val urgency = urgencyOf(daysRemaining)
    val colors = urgencyColors(urgency)
    val text = when (urgency) {
        Urgency.NONE -> "履歴なし"
        Urgency.EXPIRED -> "期限切れ"
        else -> if (daysRemaining == 0L) "本日期限!" else "あと${daysRemaining}日"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = colors.content,
        modifier = modifier
            .background(colors.container, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * 自動解約日までの残日数を示す進捗バー。残日数バンドに応じて色が変わる(緑→黄→橙→赤)。
 * 残日数の数値は [RemainingDaysBadge] が併記するため、ここでは数値ラベルは持たない。
 */
@Composable
fun ExpiryProgressBar(
    daysRemaining: Long,
    expiryPeriodDays: Int,
    modifier: Modifier = Modifier,
) {
    val progress = (daysRemaining.toFloat() / expiryPeriodDays).coerceIn(0f, 1f)
    val colors = urgencyColors(urgencyOf(daysRemaining))
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
        color = colors.content,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

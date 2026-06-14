package com.fuku856.povomanager.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class Urgency { SAFE, WARNING, DANGER, EXPIRED, NONE }

fun urgencyOf(daysRemaining: Long?): Urgency = when {
    daysRemaining == null -> Urgency.NONE
    daysRemaining < 0 -> Urgency.EXPIRED
    daysRemaining <= 7 -> Urgency.DANGER
    daysRemaining <= 30 -> Urgency.WARNING
    else -> Urgency.SAFE
}

data class UrgencyColors(val container: Color, val content: Color)

@Composable
fun urgencyColors(urgency: Urgency): UrgencyColors {
    val dark = isSystemInDarkTheme()
    return when (urgency) {
        Urgency.SAFE ->
            if (dark) UrgencyColors(Color(0xFF1B3A22), Color(0xFF8BD99B))
            else UrgencyColors(Color(0xFFD7F2DC), Color(0xFF1B5E2A))
        Urgency.WARNING ->
            if (dark) UrgencyColors(Color(0xFF453A14), Color(0xFFF0CE63))
            else UrgencyColors(Color(0xFFFBEFC9), Color(0xFF6D5605))
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

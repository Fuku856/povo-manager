package com.fuku856.povomanager.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fuku856.povomanager.MainActivity
import com.fuku856.povomanager.R
import com.fuku856.povomanager.data.LineRepository
import com.fuku856.povomanager.data.settings.SettingsRepository
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.domain.toStatus
import com.fuku856.povomanager.ui.common.displayName
import com.fuku856.povomanager.ui.common.toDisplayString
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate

class PovoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PovoWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun lineRepository(): LineRepository
    fun settingsRepository(): SettingsRepository
}

class PovoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val lines = entryPoint.lineRepository().getLinesWithPurchases()
        val settings = entryPoint.settingsRepository().current()
        val today = LocalDate.now()
        val statuses = lines.map { it.toStatus(settings, today) }
        // 手動並び替えONのときはDAOが返すsortOrder順をそのまま使い、OFFのときは期限の早い順。
        val ordered = if (settings.widgetManualOrder) {
            statuses
        } else {
            statuses.sortedWith(compareBy(nullsLast()) { it.daysRemaining })
        }

        provideContent {
            GlanceTheme {
                WidgetContent(ordered)
            }
        }
    }
}

@Composable
private fun WidgetContent(statuses: List<LineStatus>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            "povo期限",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))

        if (statuses.isEmpty()) {
            Text(
                "回線が未登録です",
                style = TextStyle(fontSize = 15.sp, color = GlanceTheme.colors.onSurface),
            )
            return@Column
        }

        val top = statuses.first()
        Text(
            top.line.displayName,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        RemainingChip(top.daysRemaining, large = true)
        top.expiryDate?.let {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                "期限: ${it.toDisplayString()}",
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        }

        statuses.drop(1).take(3).forEach { status ->
            Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    status.line.displayName,
                    style = TextStyle(fontSize = 15.sp, color = GlanceTheme.colors.onSurface),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(8.dp))
                RemainingChip(status.daysRemaining, large = false)
            }
        }
    }
}

/** 残日数の角丸チップ。アプリ本体の RemainingDaysBadge に見た目を寄せる。 */
@Composable
private fun RemainingChip(daysRemaining: Long?, large: Boolean) {
    val colors = chipColors(daysRemaining)
    Row(
        modifier = GlanceModifier
            .background(colors.container)
            .cornerRadius(if (large) 14.dp else 10.dp)
            .padding(
                horizontal = if (large) 12.dp else 8.dp,
                vertical = if (large) 6.dp else 3.dp,
            ),
    ) {
        Text(
            remainingText(daysRemaining),
            style = TextStyle(
                fontSize = if (large) 26.sp else 14.sp,
                fontWeight = if (large) FontWeight.Bold else FontWeight.Medium,
                color = colors.content,
            ),
        )
    }
}

private fun remainingText(daysRemaining: Long?): String = when {
    daysRemaining == null -> "履歴なし"
    daysRemaining < 0 -> "期限切れ?"
    daysRemaining == 0L -> "本日期限!"
    else -> "あと${daysRemaining}日"
}

private data class ChipColors(val container: ColorProvider, val content: ColorProvider)

@Composable
private fun chipColors(daysRemaining: Long?): ChipColors = when {
    daysRemaining == null ->
        ChipColors(GlanceTheme.colors.surfaceVariant, GlanceTheme.colors.onSurfaceVariant)
    daysRemaining <= 7 ->
        ChipColors(GlanceTheme.colors.errorContainer, GlanceTheme.colors.onErrorContainer)
    else ->
        ChipColors(GlanceTheme.colors.primaryContainer, GlanceTheme.colors.onPrimaryContainer)
}

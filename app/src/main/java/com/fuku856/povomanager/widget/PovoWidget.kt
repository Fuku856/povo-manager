package com.fuku856.povomanager.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fuku856.povomanager.MainActivity
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
        val statuses = lines
            .map { it.toStatus(settings, today) }
            .sortedWith(compareBy(nullsLast()) { it.daysRemaining })

        provideContent {
            GlanceTheme {
                WidgetContent(statuses)
            }
        }
    }
}

@Composable
private fun WidgetContent(statuses: List<LineStatus>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            "povo期限",
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))

        if (statuses.isEmpty()) {
            Text(
                "回線が未登録です",
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
            )
            return@Column
        }

        val top = statuses.first()
        Text(
            top.line.displayName,
            style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurface),
            maxLines = 1,
        )
        Text(
            remainingText(top.daysRemaining),
            style = TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = remainingColor(top.daysRemaining),
            ),
        )
        top.expiryDate?.let {
            Text(
                "期限: ${it.toDisplayString()}",
                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        }

        statuses.drop(1).take(3).forEach { status ->
            Spacer(GlanceModifier.height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    status.line.displayName,
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    remainingText(status.daysRemaining),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = remainingColor(status.daysRemaining),
                    ),
                )
            }
        }
    }
}

private fun remainingText(daysRemaining: Long?): String = when {
    daysRemaining == null -> "履歴なし"
    daysRemaining < 0 -> "期限切れ?"
    daysRemaining == 0L -> "本日期限!"
    else -> "あと${daysRemaining}日"
}

@Composable
private fun remainingColor(daysRemaining: Long?): ColorProvider = when {
    daysRemaining == null -> GlanceTheme.colors.onSurfaceVariant
    daysRemaining <= 7 -> GlanceTheme.colors.error
    else -> GlanceTheme.colors.onSurface
}

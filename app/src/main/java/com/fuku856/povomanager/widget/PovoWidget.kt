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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
    // 注: ルートに .clickable を付けると全面がタップ領域になり、LazyColumn(ListView)の
    // スクロールを奪うランチャーがある。クリックは各行(と空表示)に個別に持たせる。
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(16.dp),
    ) {
        Text(
            "povo期限",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))

        if (statuses.isEmpty()) {
            Text(
                "回線が未登録です",
                style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            )
            return@Column
        }

        // 全回線を均一な行(回線名 + 右に残日数)で表示する。ウィジェットを大きくすると
        // 残りの高さを使って多くの回線が並び、収まらない分はスクロールできる。
        LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            items(statuses) { status ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        status.line.displayName,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurface,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    RemainingChip(status.daysRemaining)
                }
            }
        }
    }
}

/** 残日数の角丸チップ。回線名の右に表示する。アプリ本体の RemainingDaysBadge に見た目を寄せる。 */
@Composable
private fun RemainingChip(daysRemaining: Long?) {
    val colors = chipColors(daysRemaining)
    Row(
        modifier = GlanceModifier
            .background(colors.container)
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            remainingText(daysRemaining),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
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

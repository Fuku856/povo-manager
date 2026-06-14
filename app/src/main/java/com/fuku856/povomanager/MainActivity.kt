package com.fuku856.povomanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fuku856.povomanager.notifications.NotificationHelper
import com.fuku856.povomanager.ui.PovoApp
import com.fuku856.povomanager.ui.theme.PovoManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // 通知・ウィジェットのタップで開く回線ID。singleTop のため、起動中に届く新しい
    // Intent は onNewIntent で受け取り、状態更新で詳細画面へ遷移させる。
    private var deepLinkLineId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        deepLinkLineId = intent.lineIdExtra()
        setContent {
            PovoManagerTheme {
                PovoApp(
                    deepLinkLineId = deepLinkLineId,
                    onDeepLinkConsumed = { deepLinkLineId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkLineId = intent.lineIdExtra()
    }

    private fun Intent.lineIdExtra(): Long? =
        getLongExtra(NotificationHelper.EXTRA_LINE_ID, -1L).takeIf { it != -1L }
}

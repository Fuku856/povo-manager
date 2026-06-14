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
        deepLinkLineId = consumeLineIdExtra()
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
        deepLinkLineId = consumeLineIdExtra()
    }

    /**
     * Intent から回線IDを読み取り、読み取れたら extra を消す。
     * 残しておくと画面回転などの再生成時に onCreate が再度読み込んで意図せず再遷移するため。
     */
    private fun consumeLineIdExtra(): Long? {
        val id = intent.getLongExtra(NotificationHelper.EXTRA_LINE_ID, -1L).takeIf { it != -1L }
        if (id != null) intent.removeExtra(NotificationHelper.EXTRA_LINE_ID)
        return id
    }
}

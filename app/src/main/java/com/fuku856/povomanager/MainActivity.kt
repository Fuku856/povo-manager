package com.fuku856.povomanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fuku856.povomanager.notifications.NotificationHelper
import com.fuku856.povomanager.ui.PovoApp
import com.fuku856.povomanager.ui.theme.PovoManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 通知タップから起動された場合は該当回線の詳細を開く
        val initialLineId = intent.getLongExtra(NotificationHelper.EXTRA_LINE_ID, -1L)
            .takeIf { it != -1L }
        setContent {
            PovoManagerTheme {
                PovoApp(initialLineId = initialLineId)
            }
        }
    }
}

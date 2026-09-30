package com.fuku856.povomanager.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

/**
 * スナックバー(バナー)を左右どちらかにスワイプして閉じられる [SnackbarHost]。
 * アプリ全体で `SnackbarHost(state)` の代わりに使う。
 *
 * スワイプで確定した時点で [androidx.compose.material3.SnackbarData.dismiss] を呼び、
 * ホストに「閉じる」を通知する。各スナックバーごとにスワイプ状態をリセットするため
 * [key] で `data` を境界にする。
 *
 * ダークモードでは既定の inverse 配色(明るい帯)が暗い画面で眩しいため、暗い帯にする。
 */
@Composable
fun SwipeDismissSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val colors = MaterialTheme.colorScheme
    SnackbarHost(hostState, modifier) { data ->
        key(data) {
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value != SwipeToDismissBoxValue.Settled) {
                        data.dismiss()
                        true
                    } else {
                        false
                    }
                },
            )
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
            ) {
                Snackbar(
                    data,
                    containerColor = if (dark) colors.surfaceContainerHighest else SnackbarDefaults.color,
                    contentColor = if (dark) colors.onSurface else SnackbarDefaults.contentColor,
                    actionColor = if (dark) colors.primary else SnackbarDefaults.actionColor,
                )
            }
        }
    }
}

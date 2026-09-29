package com.fuku856.povomanager.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * アプリ全体で1つのスナックバー(PovoApp で NavHost の上に重ねて表示)の下に空ける高さ。
 * 各画面の Scaffold の外にあるため、FAB を避ける配置は Scaffold に任せられない。
 * FAB のある画面が [ReserveSnackbarBottomSpace] で登録し、トーストを FAB の上へ持ち上げる。
 */
@Stable
class SnackbarBottomSpace {
    var height: Dp by mutableStateOf(0.dp)
}

val LocalSnackbarBottomSpace = staticCompositionLocalOf { SnackbarBottomSpace() }

/** 表示されている間だけ、アプリ全体のスナックバーを [height] だけ持ち上げる。 */
@Composable
fun ReserveSnackbarBottomSpace(height: Dp) {
    val space = LocalSnackbarBottomSpace.current
    DisposableEffect(space, height) {
        space.height = height
        onDispose { space.height = 0.dp }
    }
}

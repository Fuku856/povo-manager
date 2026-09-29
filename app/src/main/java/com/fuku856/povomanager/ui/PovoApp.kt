package com.fuku856.povomanager.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fuku856.povomanager.ui.archived.ArchivedLinesScreen
import com.fuku856.povomanager.ui.common.LocalSnackbarBottomSpace
import com.fuku856.povomanager.ui.common.SnackbarBottomSpace
import com.fuku856.povomanager.ui.common.SwipeDismissSnackbarHost
import com.fuku856.povomanager.ui.common.UndoController
import com.fuku856.povomanager.ui.common.showUndoSnackbar
import com.fuku856.povomanager.ui.home.HomeScreen
import com.fuku856.povomanager.ui.lineedit.LineEditScreen
import com.fuku856.povomanager.ui.linedetail.LineDetailScreen
import com.fuku856.povomanager.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object ArchivedRoute

@Serializable
data class LineDetailRoute(val lineId: Long)

/** lineId = -1 で新規追加 */
@Serializable
data class LineEditRoute(val lineId: Long = -1L)

@Serializable
data object SettingsRoute

@Composable
fun PovoApp(
    undoController: UndoController,
    deepLinkLineId: Long? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarBottomSpace = remember { SnackbarBottomSpace() }

    NotificationPermissionEffect()

    // 通知/ウィジェットから渡された回線IDで詳細へ遷移する。消費後にnullへ戻すことで、
    // 同じ回線を続けてタップしても再遷移できるようにする。
    // どの画面から来てもホーム起点に正規化(戻るでホームへ)し、画面が積み重ならないようにする。
    LaunchedEffect(deepLinkLineId) {
        if (deepLinkLineId != null) {
            navController.navigate(LineDetailRoute(deepLinkLineId)) {
                popUpTo(HomeRoute) { inclusive = false }
                launchSingleTop = true
            }
            onDeepLinkConsumed()
        }
    }

    // 各画面の操作後に依頼された取り消しトーストを出す。新しい依頼が来たら表示中のものを置き換える。
    // 取り消しは UndoController がアプリ寿命のスコープで行うため、操作した画面を離れていても効く。
    LaunchedEffect(undoController, snackbarHostState) {
        undoController.requests.collectLatest { action ->
            val result = snackbarHostState.showUndoSnackbar(action.message, action.actionLabel)
            if (result == SnackbarResult.ActionPerformed) undoController.undo(action)
        }
    }

    // 画面遷移は横スライド(進む=右から、戻る=左へ)で短めの duration とし、
    // もっさり感を解消する。pop 遷移を定義することで予測型戻る(predictive back)時に
    // 前画面がプレビュー表示される(AndroidManifest の enableOnBackInvokedCallback と併用)。
    val transitionDuration = 280
    CompositionLocalProvider(LocalSnackbarBottomSpace provides snackbarBottomSpace) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(transitionDuration)) },
                exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(transitionDuration)) },
                popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(transitionDuration)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(transitionDuration)) },
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onLineClick = { navController.navigate(LineDetailRoute(it)) },
                        onAddLine = { navController.navigate(LineEditRoute()) },
                        onSettings = { navController.navigate(SettingsRoute) },
                        onShowArchived = { navController.navigate(ArchivedRoute) },
                    )
                }
                composable<ArchivedRoute> {
                    ArchivedLinesScreen(
                        onBack = { navController.popBackStack() },
                        onLineClick = { navController.navigate(LineDetailRoute(it)) },
                    )
                }
                composable<LineDetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<LineDetailRoute>()
                    LineDetailScreen(
                        lineId = route.lineId,
                        onEdit = { navController.navigate(LineEditRoute(route.lineId)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<LineEditRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<LineEditRoute>()
                    LineEditScreen(
                        lineId = route.lineId,
                        onDone = { navController.popBackStack() },
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }

            // 取り消しトーストはアプリ全体で1つにまとめ、NavHost の外側(上)に重ねて出す。
            // 画面を移ってもトーストが消えず、そのまま「取り消す」を押せる。
            val bottomSpace by animateDpAsState(snackbarBottomSpace.height, label = "snackbarBottomSpace")
            SwipeDismissSnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                    .padding(bottom = bottomSpace),
            )
        }
    }
}

/** Android 13+ で初回起動時に通知権限をリクエストする */
@Composable
private fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 拒否されても機能自体は使える */ }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

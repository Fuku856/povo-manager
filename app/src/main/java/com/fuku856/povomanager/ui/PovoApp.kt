package com.fuku856.povomanager.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fuku856.povomanager.ui.home.HomeScreen
import com.fuku856.povomanager.ui.lineedit.LineEditScreen
import com.fuku856.povomanager.ui.linedetail.LineDetailScreen
import com.fuku856.povomanager.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class LineDetailRoute(val lineId: Long)

/** lineId = -1 で新規追加 */
@Serializable
data class LineEditRoute(val lineId: Long = -1L)

@Serializable
data object SettingsRoute

@Composable
fun PovoApp(initialLineId: Long? = null) {
    val navController = rememberNavController()

    NotificationPermissionEffect()

    LaunchedEffect(initialLineId) {
        if (initialLineId != null) {
            navController.navigate(LineDetailRoute(initialLineId))
        }
    }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onLineClick = { navController.navigate(LineDetailRoute(it)) },
                onAddLine = { navController.navigate(LineEditRoute()) },
                onSettings = { navController.navigate(SettingsRoute) },
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

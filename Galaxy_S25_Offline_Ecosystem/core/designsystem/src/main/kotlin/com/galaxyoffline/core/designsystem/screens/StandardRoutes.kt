package com.galaxyoffline.core.designsystem.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.galaxyoffline.core.designsystem.AppInfo

object GalaxyRoutes {
    const val SETTINGS = "ayarlar"
    const val ABOUT = "hakkinda"
}

/** Geri yığınında önceki ekran varsa geri döner (çift dokunmada boş ekranı önler). */
fun NavHostController.safeBack() {
    if (previousBackStackEntry != null) popBackStack()
}

/** Her uygulamaya standart Ayarlar ve Hakkında ekranlarını ekler. */
fun NavGraphBuilder.galaxyStandardScreens(
    navController: NavHostController,
    info: AppInfo,
    appSettings: (@Composable ColumnScope.() -> Unit)? = null,
) {
    composable(GalaxyRoutes.SETTINGS) {
        GalaxySettingsScreen(
            onBack = navController::safeBack,
            onOpenAbout = { navController.navigate(GalaxyRoutes.ABOUT) { launchSingleTop = true } },
            appSettings = appSettings,
        )
    }
    composable(GalaxyRoutes.ABOUT) { GalaxyAboutScreen(info, onBack = navController::safeBack) }
}

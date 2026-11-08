package com.galaxyoffline.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.PermissionUse
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.habittracker.ui.DetailScreen
import com.galaxyoffline.habittracker.ui.HabitViewModel
import com.galaxyoffline.habittracker.ui.TodayScreen

val Accent = AppAccent(primaryLight = Color(0xFF558B2F), primaryDark = Color(0xFFB5DA8C))

val Info = AppInfo(
    name = "Alışkanlık Takipçisi",
    description = "Günlük alışkanlıklar, seri takibi, ısı haritası ve isteğe bağlı hatırlatmalar.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.TaskAlt,
    permissions = listOf(PermissionUse("Bildirimler", "İsteğe bağlı günlük hatırlatma için."),
        PermissionUse("Açılışta çalıştırma", "Telefon yeniden başlayınca hatırlatmaları yeniden kurmak için.")),
    limitations = listOf("Hatırlatmalar tam zamanlı alarm izni olmadan kurulur; Android pil tasarrufu nedeniyle birkaç dakika gecikebilir.",
        "Seri, yalnızca seçtiğiniz günler üzerinden hesaplanır; planlı olmayan günler seriyi bozmaz."),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setGalaxyContent(Accent) { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: HabitViewModel = viewModel()
    NavHost(nav, startDestination = "bugun") {
        composable("bugun") { TodayScreen(vm, onOpen = { nav.navigate("detay/$it") { launchSingleTop = true } },
            onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) }
        composable("detay/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { DetailScreen(vm, it.arguments?.getLong("id") ?: 0, nav::safeBack) }
        galaxyStandardScreens(nav, Info)
    }
}

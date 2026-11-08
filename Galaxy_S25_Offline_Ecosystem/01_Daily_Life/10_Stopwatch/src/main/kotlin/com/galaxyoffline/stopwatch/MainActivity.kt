package com.galaxyoffline.stopwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.SwitchRow
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.stopwatch.ui.HistoryScreen
import com.galaxyoffline.stopwatch.ui.StopwatchScreen
import com.galaxyoffline.stopwatch.ui.StopwatchViewModel

val Accent = AppAccent(primaryLight = Color(0xFF455A64), primaryDark = Color(0xFFB8CAD4))

val Info = AppInfo(
    name = "Kronometre",
    description = "Turlu, uygulama kapansa veya telefon yeniden başlasa bile süreyi koruyan, tamamen çevrimdışı kronometre.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.Timer,
    limitations = listOf("Çalışan kronometre için kalıcı bildirim yoktur (bildirim izni istememek için); süre arka planda doğru sayılmaya devam eder."),
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
    val vm: StopwatchViewModel = viewModel()
    NavHost(nav, startDestination = "kronometre") {
        composable("kronometre") {
            StopwatchScreen(vm, onHistory = { nav.navigate("gecmis") { launchSingleTop = true } },
                onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } })
        }
        composable("gecmis") { HistoryScreen(vm, onBack = nav::safeBack) }
        galaxyStandardScreens(nav, Info) {
            val s by vm.settings.collectAsStateWithLifecycle()
            SwitchRow("Çalışırken ekranı açık tut", s.keepScreenOn, { vm.setKeepScreenOn(it) }, "Kronometre çalışırken ekran kararmaz.")
            SwitchRow("Titreşimli geri bildirim", s.haptics, { vm.setHaptics(it) })
        }
    }
}

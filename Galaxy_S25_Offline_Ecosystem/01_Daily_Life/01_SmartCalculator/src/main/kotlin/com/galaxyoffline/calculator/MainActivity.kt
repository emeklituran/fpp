package com.galaxyoffline.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.calculator.CalcSettingsSection
import com.galaxyoffline.core.calculator.CalculatorViewModel
import com.galaxyoffline.core.calculator.HistoryScreen
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent

val Accent = AppAccent(primaryLight = Color(0xFF1565C0), primaryDark = Color(0xFF9CC9FF))

val Info = AppInfo(
    name = "Akıllı Hesap Makinesi",
    description = "Canlı sonuç önizlemeli, akıllı yüzde ve bellek tuşlu, tamamen çevrimdışı hesap makinesi.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.Calculate,
    limitations = listOf("Sonuçlar 12 anlamlı basamakla gösterilir (çift duyarlıklı kayan nokta)."),
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
    val vm: CalculatorViewModel = viewModel()
    NavHost(nav, startDestination = "hesap") {
        composable("hesap") {
            CalculatorScreen(
                vm = vm,
                onOpenHistory = { nav.navigate("gecmis") { launchSingleTop = true } },
                onOpenSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable("gecmis") { HistoryScreen(vm, onBack = nav::safeBack) }
        galaxyStandardScreens(nav, Info) { CalcSettingsSection(vm) }
    }
}

package com.galaxyoffline.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.calendar.ui.CalendarScreen
import com.galaxyoffline.calendar.ui.CalendarViewModel
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.SwitchRow
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent

val Accent = AppAccent(primaryLight = Color(0xFFC62828), primaryDark = Color(0xFFFFB4AB))

val Info = AppInfo(
    name = "Takvim",
    description = "Ay görünümü, ajanda ve tekrarlı etkinliklerle tamamen çevrimdışı yerel takvim.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.CalendarMonth,
    limitations = listOf(
        "Sistem takvimi (Google/Samsung hesapları) okunmaz; etkinlikler yalnızca bu uygulamada tutulur.",
        "Dinî bayramlar her yıl değiştiği ve doğrulanmış çevrimdışı veri bulunmadığı için gösterilmez; yalnızca sabit tarihli resmî tatiller gösterilir.",
        "Aylık tekrarda 31'inde başlayan etkinlik kısa aylarda ayın son gününe düşer.",
        "Etkinlik bildirimi yoktur; zamanlı uyarı için Hatırlatıcı uygulamasını kullanın.",
    ),
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
    val vm: CalendarViewModel = viewModel()
    NavHost(nav, startDestination = "takvim") {
        composable("takvim") { CalendarScreen(vm, onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) }
        galaxyStandardScreens(nav, Info) {
            val s by vm.state.collectAsStateWithLifecycle()
            SwitchRow("Resmî tatilleri göster", s.showHolidays, { vm.setShowHolidays(it) }, "Sabit tarihli resmî tatil ve bayramlar.")
        }
    }
}

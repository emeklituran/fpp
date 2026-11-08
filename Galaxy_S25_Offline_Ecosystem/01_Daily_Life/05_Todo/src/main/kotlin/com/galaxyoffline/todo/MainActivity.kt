package com.galaxyoffline.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.todo.ui.ListsScreen
import com.galaxyoffline.todo.ui.TodoScreen
import com.galaxyoffline.todo.ui.TodoViewModel

val Accent = AppAccent(primaryLight = Color(0xFF2E7D32), primaryDark = Color(0xFF94D88E))

val Info = AppInfo(
    name = "Yapılacaklar",
    description = "Listeler, son tarih, öncelik ve akıllı filtrelerle tamamen çevrimdışı yapılacaklar uygulaması.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.TaskAlt,
    limitations = listOf("Görev bildirimleri yoktur (izin gerektirmemek için); zamanlı uyarı için Hatırlatıcı uygulamasını kullanın."),
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
    val vm: TodoViewModel = viewModel()
    NavHost(nav, startDestination = "gorevler") {
        composable("gorevler") {
            TodoScreen(vm, onManageLists = { nav.navigate("listeler") { launchSingleTop = true } },
                onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } })
        }
        composable("listeler") { ListsScreen(vm, onBack = nav::safeBack) }
        galaxyStandardScreens(nav, Info)
    }
}

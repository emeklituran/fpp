package com.galaxyoffline.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.SwitchRow
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.notes.ui.EditorScreen
import com.galaxyoffline.notes.ui.EditorViewModel
import com.galaxyoffline.notes.ui.NotesListScreen
import com.galaxyoffline.notes.ui.NotesViewModel

val Accent = AppAccent(primaryLight = Color(0xFFA86200), primaryDark = Color(0xFFFFC46B))

val Info = AppInfo(
    name = "Not Defteri",
    description = "Sabitleme, renk, arşiv, çöp kutusu ve aramalı, tamamen çevrimdışı not defteri.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.StickyNote2,
    limitations = listOf("Notlar şifrelenmez; hassas bilgiler için şifreli not kasası (Güvenlik kategorisi) planlanmıştır."),
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
    val listVm: NotesViewModel = viewModel()
    NavHost(nav, startDestination = "notlar") {
        composable("notlar") {
            NotesListScreen(
                vm = listVm,
                onOpen = { id -> nav.navigate("not?id=$id") { launchSingleTop = true } },
                onNew = { nav.navigate("not?id=-1") { launchSingleTop = true } },
                onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable("not?id={id}", arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
            val vm: EditorViewModel = viewModel()
            EditorScreen(vm, onClose = nav::safeBack)
        }
        galaxyStandardScreens(nav, Info) {
            val s by listVm.state.collectAsStateWithLifecycle()
            SwitchRow("Izgara görünümü", s.grid, { listVm.setGrid(it) }, "Notları iki sütunda göster.")
        }
    }
}

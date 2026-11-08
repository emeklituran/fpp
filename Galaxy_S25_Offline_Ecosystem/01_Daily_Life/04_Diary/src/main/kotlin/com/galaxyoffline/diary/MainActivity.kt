package com.galaxyoffline.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.diary.ui.DiaryListScreen
import com.galaxyoffline.diary.ui.DiaryListViewModel
import com.galaxyoffline.diary.ui.EntryScreen
import com.galaxyoffline.diary.ui.EntryViewModel
import com.galaxyoffline.diary.ui.StatsScreen
import java.time.LocalDate

val Accent = AppAccent(primaryLight = Color(0xFFAD1457), primaryDark = Color(0xFFFFB1C8))

val Info = AppInfo(
    name = "Kişisel Günlük",
    description = "Ruh hali kayıtlı, aranabilir ve istatistikli kişisel günlük. Kayıtlar yalnızca bu cihazda tutulur.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.AutoMirrored.Outlined.MenuBook,
    limitations = listOf("Uygulama kilidi (biyometrik) bu sürümde yoktur; kayıtlar şifrelenmez."),
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
    val listVm: DiaryListViewModel = viewModel()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) listVm.export(uri)
    }
    NavHost(nav, startDestination = "liste") {
        composable("liste") {
            DiaryListScreen(
                vm = listVm,
                onOpen = { nav.navigate("kayit?id=$it") { launchSingleTop = true } },
                onNew = { nav.navigate("kayit?id=-1") { launchSingleTop = true } },
                onStats = { nav.navigate("istatistik") { launchSingleTop = true } },
                onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable("kayit?id={id}", arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
            val vm: EntryViewModel = viewModel()
            EntryScreen(vm, onClose = nav::safeBack)
        }
        composable("istatistik") { StatsScreen(listVm, onBack = nav::safeBack) }
        galaxyStandardScreens(nav, Info) {
            ActionRow("Günlüğü dışa aktar", subtitle = "Tüm kayıtları .txt dosyası olarak kaydet", icon = Icons.Outlined.FileDownload) {
                exporter.launch("gunluk-${LocalDate.now()}.txt")
            }
        }
    }
}

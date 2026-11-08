package com.galaxyoffline.incomeexpense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.incomeexpense.ui.BudgetScreen
import com.galaxyoffline.incomeexpense.ui.HomeScreen
import com.galaxyoffline.incomeexpense.ui.MoneyViewModel
import java.time.LocalDate

val Accent = AppAccent(primaryLight = Color(0xFF00695C), primaryDark = Color(0xFF80CBC4))

val Info = AppInfo(
    name = "Gelir Gider",
    description = "Kategorili gelir-gider kaydı, aylık özet ve bütçe sınırlarıyla tamamen çevrimdışı para takibi.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.AccountBalanceWallet,
    limitations = listOf("Banka hesaplarıyla senkronizasyon yoktur (internet yok); kayıtlar elle girilir.", "Tek para birimi (₺)."),
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
    val vm: MoneyViewModel = viewModel()
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { if (it != null) vm.exportCsv(it) }
    NavHost(nav, startDestination = "ana") {
        composable("ana") { HomeScreen(vm, onBudgets = { nav.navigate("butce") { launchSingleTop = true } },
            onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) }
        composable("butce") { BudgetScreen(vm, onBack = nav::safeBack) }
        galaxyStandardScreens(nav, Info) {
            ActionRow("CSV olarak dışa aktar", subtitle = "Excel ile açılabilir (Türkçe biçim)", icon = Icons.Outlined.FileDownload) {
                csv.launch("gelir-gider-${LocalDate.now()}.csv")
            }
        }
    }
}

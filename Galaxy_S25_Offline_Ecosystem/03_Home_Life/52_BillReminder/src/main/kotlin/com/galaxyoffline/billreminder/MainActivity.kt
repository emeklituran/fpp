package com.galaxyoffline.billreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.galaxyoffline.billreminder.ui.BillViewModel
import com.galaxyoffline.billreminder.ui.BillsScreen
import com.galaxyoffline.billreminder.ui.HistoryScreen
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.PermissionUse
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent

val Accent = AppAccent(primaryLight = Color(0xFF4E342E), primaryDark = Color(0xFFD7B8A8))

val Info = AppInfo(
    name = "Fatura ve Abonelik",
    description = "Fatura ve abonelik takibi: son ödeme hatırlatması, ödendi işareti, aylık/yıllık maliyet özeti.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.ReceiptLong,
    permissions = listOf(PermissionUse("Bildirimler", "Son ödeme günü yaklaşınca hatırlatmak için."),
        PermissionUse("Açılışta çalıştırma", "Telefon yeniden başlayınca günlük kontrolü yeniden kurmak için.")),
    limitations = listOf("Hatırlatmalar günde bir kez (yaklaşık 09:00) yapılan kontrolle gönderilir.",
        "Tutarlar sizin girdiğiniz tahmini değerlerdir; kurumlardan fatura bilgisi çekilmez (internet yok)."),
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
    val vm: BillViewModel = viewModel()
    NavHost(nav, startDestination = "liste") {
        composable("liste") { BillsScreen(vm, onHistory = { nav.navigate("gecmis/$it") { launchSingleTop = true } },
            onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) }
        composable("gecmis/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { HistoryScreen(vm, it.arguments?.getLong("id") ?: 0, nav::safeBack) }
        galaxyStandardScreens(nav, Info)
    }
}

package com.galaxyoffline.reminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.PermissionUse
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.reminder.ui.ReminderScreen
import com.galaxyoffline.reminder.ui.ReminderViewModel

val Accent = AppAccent(primaryLight = Color(0xFF00838F), primaryDark = Color(0xFF7FD6E0))

val Info = AppInfo(
    name = "Hatırlatıcı",
    description = "Tekrarlı, ertelenebilir ve yeniden başlatmaya dayanıklı, tamamen çevrimdışı hatırlatıcı.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.NotificationsActive,
    permissions = listOf(
        PermissionUse("Bildirimler", "Hatırlatma zamanı geldiğinde bildirim göstermek için (Android 13+, sizden istenir)."),
        PermissionUse("Tam zamanlı alarm", "Hatırlatmayı tam dakikasında tetiklemek için. Verilmezse Android birkaç dakika geciktirebilir."),
        PermissionUse("Açılışta çalıştırma", "Telefon yeniden başlatıldığında hatırlatmaları yeniden kurmak için."),
    ),
    limitations = listOf(
        "Samsung pil optimizasyonu bildirimleri geciktirebilir: Ayarlar > Pil > Arka plan kullanım sınırları bölümünden uygulamayı \"Hiçbir zaman uyutulmayan\" listesine ekleyin.",
        "Tam zamanlı alarm izni verilmezse Android hatırlatmayı birkaç dakika geciktirebilir.",
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
    val vm: ReminderViewModel = viewModel()
    NavHost(nav, startDestination = "liste") {
        composable("liste") { ReminderScreen(vm, onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) }
        galaxyStandardScreens(nav, Info)
    }
}

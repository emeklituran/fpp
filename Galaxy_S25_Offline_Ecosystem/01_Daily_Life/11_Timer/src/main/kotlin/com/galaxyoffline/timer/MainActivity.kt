package com.galaxyoffline.timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
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
import com.galaxyoffline.timer.ui.TimerScreen
import com.galaxyoffline.timer.ui.TimerViewModel

val Accent = AppAccent(primaryLight = Color(0xFFE65100), primaryDark = Color(0xFFFFCC80))

val Info = AppInfo(
    name = "Zamanlayıcı",
    description = "Geri sayım zamanlayıcı ve Pomodoro. Uygulama kapalıyken de süre dolunca sesli ve titreşimli uyarı verir.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.Timer,
    permissions = listOf(
        PermissionUse("Tam zamanlı alarm", "Süre dolduğunda tam zamanında uyarmak için (Android 13+ zamanlayıcılara otomatik verilir)."),
        PermissionUse("Bildirimler ve tam ekran uyarı", "Süren sayımı ve kilit ekranında \"Süre doldu\" ekranını göstermek için."),
        PermissionUse("Ön plan hizmeti (ses çalma)", "Uyarı sesi çalarken uygulamanın kapatılmaması için."),
        PermissionUse("Titreşim ve uyanık tutma", "Uyarı titreşimi ve ses çalarken işlemcinin uyumaması için."),
    ),
    limitations = listOf(
        "Aynı anda tek zamanlayıcı çalışır.",
        "Telefon yeniden başlatılırsa süren zamanlayıcı iptal olur (yeniden açılışta süre dolmuşsa uyarı verilir).",
        "Uyarı sesi 2 dakika sonra kendiliğinden susar.",
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
    val vm: TimerViewModel = viewModel()
    NavHost(nav, startDestination = "zamanlayici") {
        composable("zamanlayici") { TimerScreen(vm, onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) }
        galaxyStandardScreens(nav, Info)
    }
}

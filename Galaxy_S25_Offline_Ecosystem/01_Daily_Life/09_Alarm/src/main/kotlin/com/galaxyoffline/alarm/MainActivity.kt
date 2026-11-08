package com.galaxyoffline.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.galaxyoffline.alarm.ui.AlarmEditScreen
import com.galaxyoffline.alarm.ui.AlarmEditViewModel
import com.galaxyoffline.alarm.ui.AlarmListScreen
import com.galaxyoffline.alarm.ui.AlarmListViewModel
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.PermissionUse
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent

val Accent = AppAccent(primaryLight = Color(0xFF6A1B9A), primaryDark = Color(0xFFE5B5FF))

val Info = AppInfo(
    name = "Alarm",
    description = "Haftanın günlerine göre tekrarlı, ertelemeli ve kilit ekranında tam ekran çalan, tamamen çevrimdışı alarm saati.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.Alarm,
    permissions = listOf(
        PermissionUse("Tam zamanlı alarm", "Alarmı tam dakikasında çaldırmak için (Android 13+ alarm uygulamalarına otomatik verilir)."),
        PermissionUse("Bildirimler", "Çalan alarmı bildirim ve kilit ekranında göstermek için."),
        PermissionUse("Tam ekran gösterim", "Kilit ekranında tam ekran alarm göstermek için."),
        PermissionUse("Ön plan hizmeti (medya)", "Alarm sesi çalarken sistemin uygulamayı kapatmaması için."),
        PermissionUse("Titreşim", "Alarm titreşimi için."),
        PermissionUse("Uyanık tutma", "Alarm çalarken işlemcinin uykuya geçmemesi için."),
        PermissionUse("Açılışta çalıştırma", "Yeniden başlatmadan sonra alarmları yeniden kurmak için."),
    ),
    limitations = listOf(
        "Alarm sesi olarak cihazın varsayılan alarm tonu kullanılır (özel ses seçimi sonraki sürümde).",
        "Android 14+ cihazlarda tam ekran gösterim izni kapatılırsa alarm bildirim olarak çalar.",
        "Samsung pil ayarlarında uygulamayı \"Hiçbir zaman uyutulmayan uygulamalar\" listesine eklemeniz önerilir.",
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
    val listVm: AlarmListViewModel = viewModel()
    NavHost(nav, startDestination = "alarmlar") {
        composable("alarmlar") {
            AlarmListScreen(listVm, onEdit = { nav.navigate("alarm?id=$it") { launchSingleTop = true } },
                onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } })
        }
        composable("alarm?id={id}", arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
            val vm: AlarmEditViewModel = viewModel()
            AlarmEditScreen(vm, onClose = nav::safeBack)
        }
        galaxyStandardScreens(nav, Info)
    }
}

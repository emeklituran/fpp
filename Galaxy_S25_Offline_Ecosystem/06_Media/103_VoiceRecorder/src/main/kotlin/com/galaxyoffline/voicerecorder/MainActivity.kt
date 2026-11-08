package com.galaxyoffline.voicerecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.PermissionUse
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.voicerecorder.ui.RecorderScreen
import com.galaxyoffline.voicerecorder.ui.RecorderViewModel

val Accent = AppAccent(primaryLight = Color(0xFFAD1457), primaryDark = Color(0xFFFFB0CB))

val Info = AppInfo(
    name = "Ses Kaydedici",
    description = "AAC ses kaydı, oynatma, yeniden adlandırma ve paylaşma. Kayıtlar yalnızca bu cihazda saklanır.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.Mic,
    permissions = listOf(
        PermissionUse("Mikrofon", "Ses kaydı yapmak için. Yalnızca siz kayda başlattığınızda kullanılır."),
        PermissionUse("Ön plan hizmeti (mikrofon)", "Ekran kapalıyken veya başka uygulamaya geçtiğinizde kaydın kesilmemesi için."),
        PermissionUse("Bildirimler", "Kayıt sürerken durumu ve Duraklat/Durdur düğmelerini göstermek için."),
    ),
    limitations = listOf(
        "Telefon görüşmeleri kaydedilemez (Android kısıtlaması).",
        "Kayıtlar uygulamanın özel klasöründedir; uygulama kaldırılırsa silinir. Saklamak için \"Müzik klasörüne aktar\" veya \"Paylaş\" kullanın.",
        "Kayıt sırasında uygulama sistem tarafından zorla kapatılırsa o kayıt kaydedilemeyebilir.",
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
    val vm: RecorderViewModel = viewModel()
    NavHost(nav, startDestination = "kayit") {
        composable("kayit") { RecorderScreen(vm, onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) }
        galaxyStandardScreens(nav, Info) {
            val br by vm.bitrate.collectAsStateWithLifecycle()
            ActionRow("Kayıt kalitesi", subtitle = "Yüksek kalite daha çok yer kaplar (128 kbps ≈ 1 MB/dk)", icon = Icons.Outlined.Mic)
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(64_000 to "Konuşma 64", 128_000 to "Standart 128", 192_000 to "Yüksek 192").forEach { (v, l) ->
                    FilterChip(br == v, { vm.setBitrate(v) }, label = { Text("$l kbps") })
                }
            }
        }
    }
}

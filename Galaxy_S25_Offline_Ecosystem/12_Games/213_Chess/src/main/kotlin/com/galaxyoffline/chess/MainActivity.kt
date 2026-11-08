package com.galaxyoffline.chess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.chess.data.EngineHolder
import com.galaxyoffline.chess.data.EngineSettings
import com.galaxyoffline.chess.ui.EditorScreen
import com.galaxyoffline.chess.ui.EngineTestScreen
import com.galaxyoffline.chess.ui.GameScreen
import com.galaxyoffline.chess.ui.GameViewModel
import com.galaxyoffline.chess.ui.LicensesScreen
import com.galaxyoffline.chess.ui.SavedScreen
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.components.SectionHeader
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent

val Accent = AppAccent(primaryLight = Color(0xFF5D4037), primaryDark = Color(0xFFD7CCC8))

val Info = AppInfo(
    name = "Satranç",
    description = "Çevrimdışı satranç ve analiz: bilgisayara karşı ve iki kişilik oyun, Stockfish 19 ile canlı analiz, konum düzenleyici, FEN.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.Extension,
    permissions = emptyList(),
    limitations = listOf(
        "Sürükleyerek hamle bu sürümde yok; taşa ve hedef kareye dokunarak oynanır.",
        "PGN içe aktarma, açılış adları, oyun sonu hata raporu ve Türkçe hamle açıklamaları sonraki sürümlerde (1.1 ve 1.2).",
        "Motor (Stockfish 19) cihazda çalışır; analiz pili kullanır ve telefonu ısıtabilir. Uygulama arka plana geçince motor durur.",
        "Uygulama cihazda yaklaşık 190 MB yer kaplar (motorun sinir ağı dosyası ≈100 MB).",
        "Bilgisayar seviyeleri Stockfish'in Elo sınırlamasıyla yaklaşık değerlerdir.",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setGalaxyContent(Accent) { AppRoot() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: GameViewModel = viewModel()
    NavHost(nav, startDestination = "oyun") {
        composable("oyun") {
            GameScreen(vm, onEditor = { nav.navigate("duzenleyici") { launchSingleTop = true } }, onSaved = { nav.navigate("kayitlar") { launchSingleTop = true } },
                onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } })
        }
        composable("duzenleyici") { EditorScreen(vm, onBack = nav::safeBack, onDone = nav::safeBack) }
        composable("kayitlar") { SavedScreen(vm, onBack = nav::safeBack, onOpen = nav::safeBack) }
        composable("motor_testi") { EngineTestScreen(onBack = nav::safeBack) }
        composable("lisanslar") { LicensesScreen(onBack = nav::safeBack) }
        galaxyStandardScreens(nav, Info) {
            val holder = EngineHolder.get(LocalContext.current)
            val st by holder.settings.collectAsStateWithLifecycle()
            SectionHeader("Motor")
            Text("Analiz süresi (konum başına)", Modifier.padding(horizontal = 16.dp))
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1_000L to "1 sn", 3_000L to "3 sn", 5_000L to "5 sn", 10_000L to "10 sn", 0L to "Süresiz").forEach { (ms, l) ->
                    FilterChip(st.analysisMs == ms, { holder.updateSettings(st.copy(analysisMs = ms)) }, { Text(l) }, Modifier.testTag("ms_$ms"))
                }
            }
            Text("İş parçacığı (çekirdek: ${EngineSettings.CORES})", Modifier.padding(horizontal = 16.dp))
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..EngineSettings.CORES).forEach { t -> FilterChip(st.threads == t, { holder.updateSettings(st.copy(threads = t)) }, { Text("$t") }) }
            }
            Text("Karma tablosu (bellek)", Modifier.padding(horizontal = 16.dp))
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(16, 32, 64, 128, 256).forEach { h -> FilterChip(st.hashMb == h, { holder.updateSettings(st.copy(hashMb = h)) }, { Text("$h MB") }) }
            }
            ActionRow("Motor testi", subtitle = "Bu cihazda hız ve mat bulma ölçümü", icon = Icons.Outlined.Speed) { nav.navigate("motor_testi") { launchSingleTop = true } }
            ActionRow("Açık kaynak lisansları", subtitle = "GPL-3 (uygulama ve Stockfish), Apache-2.0, cburnett taşları", icon = Icons.Outlined.Description,
                modifier = Modifier.testTag("licenses")) { nav.navigate("lisanslar") { launchSingleTop = true } }
        }
    }
}

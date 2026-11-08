package com.galaxyoffline.unitconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.unitconverter.data.Units
import com.galaxyoffline.unitconverter.ui.ConverterScreen
import com.galaxyoffline.unitconverter.ui.HomeScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Accent = AppAccent(primaryLight = Color(0xFFE65100), primaryDark = Color(0xFFFFB77C))

val Info = AppInfo(
    name = "Birim Dönüştürücü",
    description = "13 kategoride 120'den fazla birim: uzunluk, ağırlık, sıcaklık, hacim, alan, hız, veri, süre, enerji, basınç, güç, açı ve mutfak ölçüleri.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.SwapHoriz,
    limitations = listOf("Para birimi dönüşümü yoktur (canlı kur internet gerektirir).", "Mutfak ölçüleri yaklaşık değerlerdir."),
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
    val prefs = GalaxyPreferences.get(LocalContext.current)
    val scope = rememberCoroutineScope()
    NavHost(nav, startDestination = "ana") {
        composable("ana") { HomeScreen(onOpen = { nav.navigate("kategori/$it") { launchSingleTop = true } },
            onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) }
        composable("kategori/{id}") { e ->
            val c = Units.category(e.arguments?.getString("id"))
            val key = stringPreferencesKey("son_birim_${c.id}")
            val last by prefs.data.map { it[key] }.collectAsStateWithLifecycle(initialValue = null)
            ConverterScreen(c, last, onUnitChange = { u -> scope.launch { prefs.edit { it[key] = u } } }, onBack = nav::safeBack)
        }
        galaxyStandardScreens(nav, Info)
    }
}

package com.galaxyoffline.qrcode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.PermissionUse
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.SwitchRow
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.qrcode.ui.CreateTab
import com.galaxyoffline.qrcode.ui.HistoryTab
import com.galaxyoffline.qrcode.ui.QrViewModel
import com.galaxyoffline.qrcode.ui.ResultSheet
import com.galaxyoffline.qrcode.ui.ScanTab
import com.galaxyoffline.qrcode.ui.Shown

val Accent = AppAccent(primaryLight = Color(0xFF37474F), primaryDark = Color(0xFFB0BEC5))

val Info = AppInfo(
    name = "QR Kod",
    description = "Çevrimdışı QR kod ve barkod okuyucu ile QR kod üretici. Görüntüler yalnızca cihazda işlenir.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.QrCode2,
    permissions = listOf(PermissionUse("Kamera", "QR kod ve barkod taramak için. Görüntüler kaydedilmez.")),
    limitations = listOf("Bağlantılar varsayılan tarayıcınızda açılır; bu uygulama internete çıkmaz.",
        "Wi-Fi ağına otomatik bağlanma yapılmaz (ek izin gerektirir); şifre kopyalanıp ayarlardan bağlanılır."),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setGalaxyContent(Accent) { AppRoot() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: QrViewModel = viewModel()
    NavHost(nav, startDestination = "ana") {
        composable("ana") {
            var tab by rememberSaveable { mutableIntStateOf(0) }
            var shown by remember { mutableStateOf<Shown?>(null) }
            val snackbar = remember { SnackbarHostState() }
            LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
            Scaffold(
                topBar = { TopAppBar(title = { Text(listOf("QR tara", "QR oluştur", "Geçmiş")[tab]) }, actions = {
                    IconButton(onClick = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) { Icon(Icons.Outlined.Settings, "Ayarlar") }
                }) },
                bottomBar = {
                    NavigationBar {
                        listOf(Triple("Tara", Icons.Outlined.QrCodeScanner, "tab_scan"), Triple("Oluştur", Icons.Outlined.QrCode2, "tab_create"),
                            Triple("Geçmiş", Icons.Outlined.History, "tab_history")).forEachIndexed { i, (l, ic, tag) ->
                            NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(ic, null) }, label = { Text(l) }, modifier = Modifier.testTag(tag))
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { p ->
                Box(Modifier.padding(p)) {
                    when (tab) {
                        0 -> ScanTab(vm, onResult = { shown = it }, paused = shown != null)
                        1 -> CreateTab(vm)
                        else -> HistoryTab(vm, onOpen = { shown = it })
                    }
                }
            }
            shown?.let { s -> ResultSheet(s.text, s.format, onDismiss = { shown = null }) }
        }
        galaxyStandardScreens(nav, Info) {
            val save by vm.saveHistory.collectAsStateWithLifecycle()
            SwitchRow("Tarama geçmişini kaydet", save, { vm.setSaveHistory(it) }, "Kapalıyken taranan kodlar saklanmaz.")
        }
    }
}

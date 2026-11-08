package com.galaxyoffline.pdfreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.IconBadge
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.SwitchRow
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.pdfreader.ui.PdfViewModel
import com.galaxyoffline.pdfreader.ui.ViewerScreen
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.ZoneId

val Accent = AppAccent(primaryLight = Color(0xFFB71C1C), primaryDark = Color(0xFFFFB4A9))

val Info = AppInfo(
    name = "PDF Okuyucu",
    description = "Android'in yerleşik PDF motoruyla hızlı, izinsiz ve tamamen çevrimdışı PDF okuyucu.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.PictureAsPdf,
    limitations = listOf("Metin seçme/arama ve form doldurma desteklenmez (yerleşik PDF motoru sayfa görüntüsü üretir).",
        "Parola korumalı PDF'ler açılamaz."),
)

class MainActivity : ComponentActivity() {
    /** "Birlikte aç" ile gelen dosya. */
    val incoming = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setGalaxyContent(Accent) { AppRoot(incoming) }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }
    private fun handle(i: Intent?) { if (i?.action == Intent.ACTION_VIEW) incoming.value = i.data }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(incoming: MutableStateFlow<Uri?>) {
    val nav = rememberNavController()
    val vm: PdfViewModel = viewModel()
    val open by vm.open.collectAsStateWithLifecycle()
    val inc by incoming.collectAsStateWithLifecycle()
    LaunchedEffect(inc) { inc?.let { vm.open(it, persist = false); incoming.value = null } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) vm.open(it, persist = true) }
    open?.let { ViewerScreen(vm, it, onClose = vm::close); return }
    NavHost(nav, startDestination = "ana") {
        composable("ana") {
            val recents by vm.recents.collectAsStateWithLifecycle()
            val loading by vm.loading.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }
            LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
            Scaffold(topBar = { TopAppBar(title = { Text("PDF Okuyucu") }, actions = {
                IconButton(onClick = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } }) { Icon(Icons.Outlined.Settings, "Ayarlar") }
            }) }, floatingActionButton = {
                ExtendedFloatingActionButton(onClick = { picker.launch(arrayOf("application/pdf")) }, icon = { Icon(Icons.Filled.FolderOpen, null) },
                    text = { Text("PDF aç") }, modifier = Modifier.testTag("fab_open"))
            }, snackbarHost = { SnackbarHost(snackbar) }) { p ->
                Column(Modifier.padding(p).fillMaxSize()) {
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (recents.isEmpty()) EmptyState(Icons.Outlined.PictureAsPdf, "Son açılan dosya yok",
                        "\"PDF aç\" ile bir dosya seçin veya dosya yöneticisinden \"Birlikte aç > PDF Okuyucu\" deyin. Depolama izni gerekmez.")
                    else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recents, key = { it.uri }) { r ->
                            val t = Instant.ofEpochMilli(r.openedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                            Card(onClick = { vm.open(Uri.parse(r.uri), persist = false) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconBadge(Icons.Outlined.PictureAsPdf)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(r.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${r.pages} sayfa · kaldığı yer ${r.lastPage + 1} · ${TrFormat.relativeDay(t)}", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { vm.remove(r) }) { Icon(Icons.Outlined.Close, "Listeden kaldır") }
                                }
                            }
                        }
                    }
                }
            }
        }
        galaxyStandardScreens(nav, Info) {
            val night by vm.night.collectAsStateWithLifecycle()
            SwitchRow("Gece modu", night, { vm.setNight(it) }, "Sayfa renklerini ters çevirerek karanlıkta okumayı kolaylaştırır.")
        }
    }
}


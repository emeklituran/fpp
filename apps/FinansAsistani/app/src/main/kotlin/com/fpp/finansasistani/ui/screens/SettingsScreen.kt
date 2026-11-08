package com.fpp.finansasistani.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fpp.finansasistani.BuildConfig
import com.fpp.finansasistani.data.ThemeMode
import com.fpp.finansasistani.ui.components.ConfirmDialog
import com.fpp.finansasistani.ui.components.SectionTitle
import com.fpp.finansasistani.util.MessageBus
import com.fpp.finansasistani.viewmodel.SettingsViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, bus: MessageBus, onOpenHelp: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val crashLog by vm.crashLog.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var confirmImport by rememberSaveable { mutableStateOf(false) }
    var showCrash by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(uri)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Ayarlar") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }

            item { SectionTitle("Görünüm") }
            item {
                SettingsCard {
                    Column(Modifier.selectableGroup()) {
                        ListItem(
                            headlineContent = { Text("Tema") },
                            leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        ThemeMode.entries.forEach { mode ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .selectable(
                                        selected = settings.themeMode == mode,
                                        onClick = { vm.setThemeMode(mode) },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = settings.themeMode == mode, onClick = null)
                                Spacer(Modifier.width(16.dp))
                                Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Dinamik renkler") },
                            supportingContent = { Text("Renkleri duvar kağıdınıza göre uyarlar (Material You).") },
                            trailingContent = {
                                Switch(checked = settings.dynamicColor, onCheckedChange = null)
                            },
                            modifier = Modifier.toggleable(
                                value = settings.dynamicColor,
                                onValueChange = vm::setDynamicColor,
                                role = Role.Switch,
                            ),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }

            item { SectionTitle("Veriler ve yedekleme") }
            item {
                SettingsCard {
                    ActionItem(
                        icon = Icons.Outlined.FileDownload,
                        title = "Yedeği dışa aktar",
                        text = "Tüm kayıtları seçtiğiniz konuma JSON dosyası olarak kaydeder.",
                        enabled = !busy,
                    ) { exportLauncher.launch("finans-asistani-yedek-${LocalDate.now()}.json") }
                    ActionItem(
                        icon = Icons.Outlined.FileUpload,
                        title = "Yedekten geri yükle",
                        text = "Mevcut verilerin yerine yedek dosyasındaki veriler yüklenir.",
                        enabled = !busy,
                    ) { confirmImport = true }
                    ActionItem(
                        icon = Icons.Outlined.DeleteForever,
                        title = "Tüm verileri sil",
                        text = "Gelir, borç, gider ve birikim kayıtlarının tamamı kalıcı olarak silinir.",
                        enabled = !busy,
                        destructive = true,
                    ) { confirmClear = true }
                }
            }

            item { SectionTitle("Gizlilik") }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("Tamamen çevrimdışı") },
                        supportingContent = {
                            Text(
                                "Bu uygulamanın internet izni yoktur; reklam, analiz veya telemetri içermez. " +
                                    "Veriler yalnızca bu cihazda saklanır ve buluta yedeklenmez.",
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            item { SectionTitle("Hakkında") }
            item {
                SettingsCard {
                    ActionItem(Icons.AutoMirrored.Outlined.HelpOutline, "Yardım ve kullanım kılavuzu", "Hesaplamalar nasıl yapılır?", onClick = onOpenHelp)
                    ListItem(
                        headlineContent = { Text("Sürüm") },
                        supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
                        leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    if (crashLog != null) {
                        ActionItem(
                            icon = Icons.Outlined.BugReport,
                            title = "Son hata kaydı",
                            text = "Uygulama daha önce beklenmedik şekilde kapandı. Kaydı görüntüleyin.",
                        ) { showCrash = true }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Tüm veriler silinsin mi?",
            text = "Bu işlem geri alınamaz. İsterseniz önce yedek alın.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = vm::clearAllData,
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmImport) {
        ConfirmDialog(
            title = "Yedek geri yüklensin mi?",
            text = "Mevcut tüm kayıtlarınız silinip yedek dosyasındaki kayıtlar yüklenecek.",
            confirmLabel = "Dosya seç",
            onConfirm = {
                try {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                } catch (e: android.content.ActivityNotFoundException) {
                    bus.post("Dosya seçici bulunamadı.")
                }
            },
            onDismiss = { confirmImport = false },
        )
    }
    if (showCrash) {
        AlertDialog(
            onDismissRequest = { showCrash = false },
            title = { Text("Son hata kaydı") },
            text = {
                SelectionContainer {
                    Text(
                        crashLog.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showCrash = false }) { Text("Kapat") } },
            dismissButton = {
                TextButton(onClick = { vm.clearCrashLog(); showCrash = false }) { Text("Kaydı sil") }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(title, color = color) },
        supportingContent = { Text(text) },
        leadingContent = { Icon(icon, contentDescription = null, tint = color) },
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

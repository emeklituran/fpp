package com.galaxyoffline.core.designsystem.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.GroupCard
import com.galaxyoffline.core.designsystem.components.IconBadge
import com.galaxyoffline.core.designsystem.components.SectionHeader
import com.galaxyoffline.core.designsystem.util.CrashLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxyAboutScreen(info: AppInfo, onBack: () -> Unit) {
    val context = LocalContext.current
    var crash by remember { mutableStateOf(CrashLogger.read(context)) }
    var showCrash by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Hakkında") }, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBadge(info.icon, size = 80.dp)
                Text(info.name, style = MaterialTheme.typography.headlineSmall)
                Text("Sürüm ${info.versionName} (${info.versionCode})", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(info.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            SectionHeader("Gizlilik")
            GroupCard {
                ActionRow("Reklam yok", subtitle = "Reklam, analiz veya telemetri SDK'sı içermez.", icon = Icons.Outlined.VerifiedUser)
                ActionRow("İnternet yok", subtitle = "İnternet izni yoktur; hiçbir veri cihaz dışına gönderilmez.", icon = Icons.Outlined.CloudOff)
                ActionRow("Veriler cihazda", subtitle = "Kayıtlar yalnızca bu uygulamanın özel klasöründe saklanır ve buluta yedeklenmez.", icon = Icons.Outlined.Lock)
                ActionRow("Ücretsiz", subtitle = "Hesap, üyelik, abonelik veya uygulama içi satın alma yoktur.", icon = Icons.Outlined.MoneyOff)
            }
            SectionHeader("İzinler")
            GroupCard {
                if (info.permissions.isEmpty()) {
                    ActionRow("Bu uygulama hiçbir izin istemez.", icon = Icons.Outlined.VerifiedUser)
                } else {
                    info.permissions.forEach { ActionRow(it.permission, subtitle = it.reason, icon = Icons.Outlined.Lock) }
                }
            }
            if (info.limitations.isNotEmpty()) {
                SectionHeader("Bilinen kısıtlamalar")
                GroupCard {
                    info.limitations.forEach { ActionRow(it, icon = Icons.Outlined.WarningAmber) }
                }
            }
            SectionHeader("Teknik")
            GroupCard {
                ActionRow("Paket adı", subtitle = info.packageName, icon = Icons.Outlined.PhoneAndroid)
                if (crash != null) {
                    ActionRow("Son hata kaydı", subtitle = "Uygulama daha önce beklenmedik şekilde kapandı. Görüntülemek için dokunun.",
                        icon = Icons.Outlined.BugReport) { showCrash = true }
                }
            }
            Text(
                "Galaxy Offline uygulama ailesinin bir parçasıdır. Kotlin, Jetpack Compose ve Material 3 ile geliştirilmiştir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )
        }
    }
    if (showCrash) {
        AlertDialog(
            onDismissRequest = { showCrash = false },
            title = { Text("Son hata kaydı") },
            text = {
                SelectionContainer {
                    Text(crash.orEmpty(), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.verticalScroll(rememberScrollState()))
                }
            },
            confirmButton = { TextButton(onClick = { showCrash = false }) { Text("Kapat") } },
            dismissButton = {
                TextButton(onClick = { CrashLogger.clear(context); crash = null; showCrash = false }) { Text("Kaydı sil") }
            },
        )
    }
}

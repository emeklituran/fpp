package com.galaxyoffline.core.designsystem.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.GroupCard
import com.galaxyoffline.core.designsystem.components.SectionHeader
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.theme.ThemeMode
import com.galaxyoffline.core.designsystem.theme.ThemeSettings
import kotlinx.coroutines.launch

/**
 * Tüm uygulamalarda ortak Ayarlar ekranı: tema (açık / koyu / AMOLED / sistem), dinamik renk ve Hakkında.
 * Uygulamaya özel ayarlar [appSettings] ile eklenir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxySettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    appSettings: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val prefs = GalaxyPreferences.get(LocalContext.current)
    val theme by prefs.theme.collectAsStateWithLifecycle(initialValue = ThemeSettings())
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("Ayarlar") }, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (appSettings != null) {
                SectionHeader("Uygulama")
                GroupCard { Column { appSettings() } }
            }
            SectionHeader("Görünüm")
            GroupCard {
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .selectable(
                                    selected = theme.mode == mode,
                                    onClick = { scope.launch { prefs.setThemeMode(mode) } },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = theme.mode == mode, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                SwitchRow(
                    title = "Dinamik renkler",
                    subtitle = "Renkleri duvar kağıdınıza göre uyarlar (Material You).",
                    checked = theme.dynamicColor,
                    onCheckedChange = { scope.launch { prefs.setDynamicColor(it) } },
                )
            }
            SectionHeader("Bilgi")
            GroupCard {
                ActionRow(
                    title = "Hakkında",
                    subtitle = "Sürüm, gizlilik ve izinler",
                    icon = Icons.Outlined.Info,
                    onClick = onOpenAbout,
                )
            }
            Spacer(Modifier.heightIn(min = 24.dp))
        }
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, subtitle: String? = null) {
    ActionRow(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        trailing = { Switch(checked = checked, onCheckedChange = null) },
    )
}

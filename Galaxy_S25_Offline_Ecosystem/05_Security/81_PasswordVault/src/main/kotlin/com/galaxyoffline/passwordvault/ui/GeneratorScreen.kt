package com.galaxyoffline.passwordvault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.crypto.PasswordGenerator
import com.galaxyoffline.core.crypto.PasswordOptions
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.screens.SwitchRow
import com.galaxyoffline.core.security.SecureClipboard
import com.galaxyoffline.core.security.StrengthMeter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(vm: VaultViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val clip by vm.settings.collectAsStateWithLifecycle()
    var length by rememberSaveable { mutableStateOf(20f) }
    var upper by rememberSaveable { mutableStateOf(true) }
    var lower by rememberSaveable { mutableStateOf(true) }
    var digits by rememberSaveable { mutableStateOf(true) }
    var symbols by rememberSaveable { mutableStateOf(true) }
    var ambiguous by rememberSaveable { mutableStateOf(true) }
    val options = PasswordOptions(length.roundToInt(), upper, lower, digits, symbols, ambiguous)
    var seed by remember { mutableStateOf(0) }
    val pw = remember(options, seed) { if (upper || lower || digits || symbols) PasswordGenerator.generate(options) else "" }
    Scaffold(topBar = { TopAppBar(title = { Text("Şifre üretici") }, navigationIcon = { BackButton(onBack) }) }) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(pw.ifEmpty { "En az bir karakter türü seçin" }, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth().padding(20.dp))
            }
            StrengthMeter(pw)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { seed++ }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Refresh, null); Text("  Yenile") }
                Button(onClick = { SecureClipboard.copy(context, "Şifre", pw, clip.clipboardSeconds); vm.messages.post("Şifre kopyalandı") },
                    enabled = pw.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.ContentCopy, null); Text("  Kopyala") }
            }
            Text("Uzunluk: ${length.roundToInt()} karakter · ${PasswordGenerator.entropyBits(options).roundToInt()} bit", style = MaterialTheme.typography.labelLarge)
            Slider(length, { length = it }, valueRange = 8f..64f, steps = 55)
            SwitchRow("Büyük harf (A-Z)", upper, { upper = it })
            SwitchRow("Küçük harf (a-z)", lower, { lower = it })
            SwitchRow("Rakam (0-9)", digits, { digits = it })
            SwitchRow("Simge (!@#…)", symbols, { symbols = it })
            SwitchRow("Karışan karakterleri çıkar", ambiguous, { ambiguous = it }, "I, l, 1, O, 0 gibi")
        }
    }
}

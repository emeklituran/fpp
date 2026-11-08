package com.galaxyoffline.unitconverter.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.math.CalcFormat
import com.galaxyoffline.unitconverter.data.UnitCategory
import com.galaxyoffline.unitconverter.data.Units
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Birim Dönüştürücü") }, actions = {
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) }) { p ->
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Units.categories, key = { it.id }) { c ->
                Card(onClick = { onOpen(c.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.testTag("cat_${c.id}")) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(c.emoji, style = MaterialTheme.typography.headlineMedium)
                        Text(c.label, style = MaterialTheme.typography.titleSmall)
                        Text("${c.units.size} birim", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(category: UnitCategory, initialUnit: String?, onUnitChange: (String) -> Unit, onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("1") }
    var fromId by rememberSaveable { mutableStateOf(category.unit(initialUnit).id) }
    val from = category.unit(fromId)
    val value = TrFormat.parseDecimal(input)
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("${category.emoji} ${category.label}") }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.padding(p).fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { s -> input = s.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(20) }, label = { Text("Değer") },
                    singleLine = true, isError = input.isNotEmpty() && value == null, textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("value"))
                Spacer(Modifier.width(8.dp))
                var open by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(open, { open = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(from.symbol, {}, readOnly = true, label = { Text("Birim") }, singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag("from_unit"))
                    ExposedDropdownMenu(open, { open = false }) {
                        category.units.forEach { u -> DropdownMenuItem(text = { Text("${u.label} (${u.symbol})") }, onClick = { fromId = u.id; onUnitChange(u.id); open = false }) }
                    }
                }
            }
            if (category.note != null) InfoBanner(category.note, Modifier.padding(vertical = 8.dp))
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(category.units, key = { it.id }) { u ->
                    val out = value?.let { category.convert(it, from, u) }
                    val text = out?.let { CalcFormat.result(it) } ?: "—"
                    Card(onClick = {
                        if (out != null) {
                            ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Sonuç", "$text ${u.symbol}"))
                            scope.launch { snackbar.showSnackbar("$text ${u.symbol} kopyalandı") }
                        }
                    }, colors = CardDefaults.cardColors(containerColor = if (u.id == from.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(u.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("$text ${u.symbol}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("out_${u.id}"))
                        }
                    }
                }
            }
        }
    }
}

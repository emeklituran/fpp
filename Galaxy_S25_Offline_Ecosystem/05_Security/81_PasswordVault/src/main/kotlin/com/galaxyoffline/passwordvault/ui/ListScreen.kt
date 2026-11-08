package com.galaxyoffline.passwordvault.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.IconBadge
import com.galaxyoffline.core.designsystem.components.SearchField
import com.galaxyoffline.passwordvault.data.ItemType
import com.galaxyoffline.passwordvault.data.VaultItem

fun ItemType.icon(): ImageVector = when (this) {
    ItemType.LOGIN -> Icons.Outlined.Key; ItemType.CARD -> Icons.Outlined.CreditCard; ItemType.WIFI -> Icons.Outlined.Wifi
    ItemType.NOTE -> Icons.AutoMirrored.Outlined.StickyNote2; ItemType.OTHER -> Icons.Outlined.Shield
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(vm: VaultViewModel, onOpen: (Long) -> Unit, onNew: () -> Unit, onGenerator: () -> Unit, onSettings: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<ItemType?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    val tr = java.util.Locale.forLanguageTag("tr-TR")
    val shown = items.filter { filter == null || it.type == filter }.filter {
        val q = query.trim().lowercase(tr)
        q.isEmpty() || it.title.lowercase(tr).contains(q) || it.username.lowercase(tr).contains(q) || it.extra.lowercase(tr).contains(q)
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Şifre Kasası") }, actions = {
                IconButton(onClick = onGenerator) { Icon(Icons.Outlined.Password, "Şifre üretici") }
                IconButton(onClick = vm::lock, modifier = Modifier.testTag("lock")) { Icon(Icons.Outlined.Lock, "Kasayı kilitle") }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") }
            })
        },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Kayıt ekle") },
            modifier = Modifier.testTag("fab_new")) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        Column(Modifier.padding(p).fillMaxSize()) {
            SearchField(query, { query = it }, "Kasada ara", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                FilterChip(filter == null, { filter = null }, label = { Text("Tümü (${items.size})") }, modifier = Modifier.padding(horizontal = 4.dp))
                ItemType.entries.forEach { t ->
                    val n = items.count { it.type == t }
                    if (n > 0) FilterChip(filter == t, { filter = if (filter == t) null else t }, label = { Text("${t.label} ($n)") },
                        modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            if (shown.isEmpty()) {
                EmptyState(Icons.Outlined.Shield, if (items.isEmpty()) "Kasa boş" else "Sonuç yok",
                    if (items.isEmpty()) "Şifrelerinizi, kart ve Wi-Fi bilgilerinizi güvenle saklayın. Hepsi bu cihazda şifreli kalır." else "Aramanızla eşleşen kayıt yok.",
                    actionLabel = if (items.isEmpty()) "İlk kaydı ekle" else null, onAction = onNew)
            } else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { ItemRow(it) { onOpen(it.id) } }
            }
        }
    }
}

@Composable
private fun ItemRow(item: VaultItem, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(item.type.icon())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = when (item.type) {
                    ItemType.CARD -> if (item.secret.length >= 4) "•••• " + item.secret.takeLast(4) else item.type.label
                    ItemType.NOTE -> "Güvenli not"
                    else -> item.username.ifBlank { item.type.label }
                }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (item.favorite) Box { Icon(Icons.Filled.Star, "Favori", tint = MaterialTheme.colorScheme.tertiary) }
        }
    }
}

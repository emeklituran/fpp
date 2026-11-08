package com.galaxyoffline.shoppinglist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.shoppinglist.data.ShoppingList
import com.galaxyoffline.shoppinglist.data.ShoppingLogic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(vm: ShoppingViewModel, onOpen: (Long) -> Unit, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ShoppingList?>(null) }
    var deleting by remember { mutableStateOf<ShoppingList?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Alışveriş Listeleri") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Yeni liste") }, modifier = Modifier.testTag("fab_new")) },
    ) { p ->
        when {
            s.loading -> LoadingBox(Modifier.padding(p))
            s.lists.isEmpty() -> Box(Modifier.padding(p)) {
                EmptyState(Icons.Outlined.ShoppingCart, "Henüz liste yok", "Market, pazar veya ev ihtiyaçları için bir liste oluşturun.",
                    actionLabel = "Liste oluştur", onAction = { adding = true })
            }
            else -> LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.lists, key = { it.id }) { l ->
                    val items = s.items[l.id].orEmpty()
                    val done = items.count { it.checked }
                    var menu by remember { mutableStateOf(false) }
                    Card(onClick = { onOpen(l.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(l.name, style = MaterialTheme.typography.titleMedium)
                                    Text(if (items.isEmpty()) "Boş liste" else "$done / ${items.size} ürün alındı",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val total = ShoppingLogic.total(items)
                                if (total > 0) Text(TrFormat.money(total), style = MaterialTheme.typography.titleSmall)
                                Box {
                                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Seçenekler") }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        DropdownMenuItem(text = { Text("Yeniden adlandır") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                            onClick = { menu = false; renaming = l })
                                        DropdownMenuItem(text = { Text("Sil") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                            onClick = { menu = false; deleting = l })
                                    }
                                }
                            }
                            if (items.isNotEmpty()) LinearProgressIndicator(progress = { done.toFloat() / items.size },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                        }
                    }
                }
            }
        }
    }
    if (adding) NameDialog("Yeni liste", "Market") { name -> adding = false; if (name != null) vm.addList(name, onOpen) }
    renaming?.let { l -> NameDialog("Yeniden adlandır", l.name) { n -> renaming = null; if (n != null) vm.renameList(l, n) } }
    deleting?.let { l -> ConfirmDialog("\"${l.name}\" silinsin mi?", "Listedeki tüm ürünler silinecek.", "Sil",
        onConfirm = { vm.deleteList(l) }, onDismiss = { deleting = null }, destructive = true) }
}

@Composable
fun NameDialog(title: String, initial: String, onResult: (String?) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = { onResult(null) }, title = { Text(title) },
        text = { OutlinedTextField(name, { name = it.take(60) }, label = { Text("Liste adı") }, singleLine = true, modifier = Modifier.testTag("list_name")) },
        confirmButton = { TextButton(onClick = { onResult(name) }, enabled = name.isNotBlank(), modifier = Modifier.testTag("list_ok")) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text("Vazgeç") } })
}

package com.galaxyoffline.shoppinglist.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.shoppinglist.data.Item
import com.galaxyoffline.shoppinglist.data.ShoppingLogic
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(vm: ShoppingViewModel, listId: Long, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val list = s.lists.firstOrNull { it.id == listId }
    val items = s.items[listId].orEmpty()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Item?>(null) }
    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(s.loading, list) { if (!s.loading && list == null) onBack() }

    fun add(text: String) { if (text.isNotBlank()) { vm.quickAdd(listId, text); input = "" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(enabled = items.isNotEmpty() && list != null, onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ShoppingLogic.shareText(list!!, items))
                        runCatching { context.startActivity(Intent.createChooser(send, "Listeyi paylaş")) }
                    }) { Icon(Icons.Outlined.Share, "Paylaş") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Diğer") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Alınanları sil") }, leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                                onClick = { menu = false; vm.clearChecked(listId) })
                            DropdownMenuItem(text = { Text("Tüm işaretleri kaldır") }, leadingIcon = { Icon(Icons.Outlined.RemoveDone, null) },
                                onClick = { menu = false; vm.uncheckAll(listId) })
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val sugg = ShoppingLogic.suggestions(s.history, input, items.map { ShoppingLogic.key(it.name) }.toSet())
                    if (sugg.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sugg.take(3).forEach { h -> SuggestionChip(onClick = { add(h.name) }, label = { Text(h.name) }) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(input, { input = it.take(80) }, placeholder = { Text("Ürün ekle (ör. süt)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { add(input) }),
                            modifier = Modifier.weight(1f).testTag("item_input"))
                        FilledIconButton(onClick = { add(input) }, enabled = input.isNotBlank(), modifier = Modifier.padding(start = 8.dp).testTag("item_add")) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Ekle")
                        }
                    }
                }
            }
        },
    ) { p ->
        if (items.isEmpty()) {
            Box(Modifier.padding(p)) { EmptyState(Icons.Outlined.ShoppingBasket, "Liste boş", "Aşağıdaki alandan ürün ekleyin. Daha önce eklediğiniz ürünler öneri olarak gelir.") }
        } else {
            LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    val total = ShoppingLogic.total(items)
                    Text(
                        "${items.count { it.checked }} / ${items.size} alındı" +
                            if (total > 0) " · Tahmini toplam ${TrFormat.money(total)} · Kalan ${TrFormat.money(ShoppingLogic.remaining(items))}" else "",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ShoppingLogic.group(items).forEach { (cat, group) ->
                    item(key = "h${cat?.name ?: "done"}") {
                        Text(cat?.let { "${it.emoji}  ${it.label}" } ?: "✅  Alınanlar (${group.size})", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                    }
                    items(group, key = { it.id }) { item ->
                        ItemRow(item, onToggle = { vm.toggle(item) }, onClick = { editing = item })
                    }
                }
            }
        }
    }
    editing?.let { item ->
        ItemDialog(item, onDismiss = { editing = null }, onSave = { vm.save(it); editing = null }, onDelete = {
            editing = null
            vm.delete(item)
            scope.launch {
                if (snackbar.showSnackbar("\"${item.name}\" silindi", "Geri al", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) vm.restore(item)
            }
        })
    }
}

@Composable
private fun ItemRow(item: Item, onToggle: () -> Unit, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(item.checked, { onToggle() })
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                Text(ShoppingLogic.quantityText(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val line = ShoppingLogic.lineTotal(item)
            if (line > 0) Text(TrFormat.money(line), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

package com.galaxyoffline.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.GroupCard
import com.galaxyoffline.todo.data.TaskList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(vm: TodoViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TaskList?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TaskList?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Listeler") }, navigationIcon = { BackButton(onBack) }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Liste ekle") }) },
    ) { p ->
        LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.lists, key = { it.id }) { l ->
                GroupCard {
                    ActionRow(l.name, subtitle = "${s.listCounts[l.id] ?: 0} açık görev", icon = Icons.AutoMirrored.Outlined.List, trailing = {
                        androidx.compose.foundation.layout.Row {
                            IconButton(onClick = { editing = l }) { Icon(Icons.Outlined.Edit, "Yeniden adlandır") }
                            IconButton(onClick = { deleting = l }) { Icon(Icons.Outlined.Delete, "Sil") }
                        }
                    })
                }
            }
        }
    }
    if (adding) NameDialog("Yeni liste", "", { vm.addList(it); adding = false }) { adding = false }
    editing?.let { l -> NameDialog("Listeyi yeniden adlandır", l.name, { vm.renameList(l, it); editing = null }) { editing = null } }
    deleting?.let { l ->
        ConfirmDialog("\"${l.name}\" silinsin mi?", "Listedeki tüm görevler de silinecek.", "Sil",
            onConfirm = { vm.deleteList(l) }, onDismiss = { deleting = null }, destructive = true)
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onOk: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { OutlinedTextField(name, { name = it.take(60) }, label = { Text("Liste adı") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onOk(name) }, enabled = name.isNotBlank()) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } })
}

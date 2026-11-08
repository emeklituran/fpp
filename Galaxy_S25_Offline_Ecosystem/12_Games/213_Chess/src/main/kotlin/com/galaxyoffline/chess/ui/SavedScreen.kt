package com.galaxyoffline.chess.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.galaxyoffline.chess.data.ChessDatabase
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.GroupCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(vm: GameViewModel, onBack: () -> Unit, onOpen: () -> Unit) {
    val dao = ChessDatabase.get(LocalContext.current).games()
    val games by dao.observe().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Kayıtlı oyunlar") }, navigationIcon = { BackButton(onBack) }) }) { pad ->
        val list = games
        if (list.isNullOrEmpty()) Box(Modifier.padding(pad)) { EmptyState(Icons.Outlined.Inventory2, "Kayıtlı oyun yok", "Oyun ekranındaki menüden \"Oyunu kaydet\" ile kaydedin.") }
        else LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
            items(list, key = { it.id }) { g ->
                GroupCard(Modifier.padding(vertical = 4.dp)) {
                    ActionRow(g.title, subtitle = "${g.moveList.size} yarım hamle · ${g.result}", modifier = Modifier.testTag("saved_${g.id}"),
                        trailing = { IconButton(onClick = { scope.launch { dao.delete(g.id) } }) { Icon(Icons.Outlined.Delete, "Sil") } }) {
                        vm.loadSaved(g); onOpen()
                    }
                }
            }
        }
    }
}

package com.galaxyoffline.notes.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.LoadingBox
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel, onClose: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var colorDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }

    val close = { vm.saveAndClose(); onClose() }
    BackHandler(onBack = close)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(noteText(s.title, s.content).toByteArray()) }
                    ?: error("açılamadı")
            }.isSuccess
            android.widget.Toast.makeText(context, if (ok) "Not dışa aktarıldı" else "Dosya yazılamadı", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val bg = noteColor(s.color)
    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = {},
                navigationIcon = { BackButton(close) },
                actions = {
                    IconButton(onClick = vm::togglePin) {
                        Icon(if (s.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (s.pinned) "Sabitlemeyi kaldır" else "Sabitle")
                    }
                    IconButton(onClick = { colorDialog = true }) { Icon(Icons.Outlined.Palette, contentDescription = "Renk") }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, noteText(s.title, s.content))
                        runCatching { context.startActivity(Intent.createChooser(send, "Notu paylaş")) }
                    }, enabled = s.title.isNotBlank() || s.content.isNotBlank()) {
                        Icon(Icons.Outlined.Share, contentDescription = "Paylaş")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Diğer") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(".txt olarak dışa aktar") }, leadingIcon = { Icon(Icons.Outlined.FileDownload, null) },
                                onClick = { menu = false; exportLauncher.launch((s.title.ifBlank { "not" }).take(60) + ".txt") })
                            if (s.id != 0L) {
                                DropdownMenuItem(text = { Text(if (s.archived) "Arşivden çıkar" else "Arşivle") },
                                    leadingIcon = { Icon(if (s.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                                    onClick = { menu = false; vm.archive(onClose) })
                                DropdownMenuItem(text = { Text("Çöp kutusuna taşı") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                    onClick = { menu = false; vm.moveToTrash(onClose) })
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            !s.loaded -> LoadingBox(Modifier.padding(padding))
            s.missing -> Box(Modifier.padding(padding)) {
                EmptyState(Icons.Outlined.Delete, "Not bulunamadı", "Bu not silinmiş olabilir.", actionLabel = "Geri dön", onAction = onClose)
            }
            else -> Column(Modifier.padding(padding).fillMaxSize().imePadding().padding(horizontal = 20.dp)) {
                val textColor = SolidColor(MaterialTheme.colorScheme.primary)
                Box {
                    if (s.title.isEmpty()) Text("Başlık", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = s.title,
                        onValueChange = vm::setTitle,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = textColor,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().testTag("note_title").semantics { contentDescription = "Başlık" },
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
                    if (s.content.isEmpty()) Text("Notunuzu yazın…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = s.content,
                        onValueChange = vm::setContent,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = textColor,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxSize().testTag("note_content").semantics { contentDescription = "Not içeriği" },
                    )
                }
                val edited = if (s.updatedAt > 0) Instant.ofEpochMilli(s.updatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime() else null
                Text(
                    buildString {
                        append("${TrFormat.wordCount(s.content)} kelime · ${s.content.length} karakter")
                        if (edited != null) append(" · Düzenlendi: ${TrFormat.relativeDay(edited.toLocalDate())} ${TrFormat.formatTime(edited.toLocalTime())}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
    if (colorDialog) {
        AlertDialog(
            onDismissRequest = { colorDialog = false },
            title = { Text("Not rengi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NoteColors.indices.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { i ->
                                Box(
                                    Modifier.size(48.dp).background(noteColor(i), CircleShape)
                                        .border(2.dp, if (i == s.color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clickable { vm.setColor(i); colorDialog = false }
                                        .semantics { contentDescription = NoteColors[i].first },
                                    contentAlignment = Alignment.Center,
                                ) { if (i == s.color) Icon(Icons.Outlined.Check, null) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { colorDialog = false }) { Text("Kapat") } },
        )
    }
}

private fun noteText(title: String, content: String) =
    if (title.isBlank()) content else if (content.isBlank()) title else "$title\n\n$content"

package com.galaxyoffline.passwordvault.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.crypto.PasswordGenerator
import com.galaxyoffline.core.crypto.PasswordOptions
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.security.PasswordField
import com.galaxyoffline.core.security.SecureClipboard
import com.galaxyoffline.core.security.StrengthMeter
import com.galaxyoffline.passwordvault.data.ItemType
import com.galaxyoffline.passwordvault.data.VaultItem
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(vm: VaultViewModel, id: Long, onClose: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val existing = items.firstOrNull { it.id == id }
    var editing by rememberSaveable { mutableStateOf(id <= 0) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (id > 0 && existing == null) { onClose(); return }
    if (editing) {
        EditItem(existing ?: VaultItem(), onCancel = { if (existing == null) onClose() else editing = false },
            onSave = { vm.save(it) { if (existing == null) onClose() else editing = false } })
        return
    }
    val item = existing!!
    val context = LocalContext.current
    val clip by vm.settings.collectAsStateWithLifecycle()
    fun copy(label: String, v: String) { SecureClipboard.copy(context, label, v, clip.clipboardSeconds); vm.messages.post("$label kopyalandı" + if (clip.clipboardSeconds > 0) " (${clip.clipboardSeconds} sn sonra silinecek)" else "") }
    Scaffold(topBar = {
        TopAppBar(title = { Text(item.title) }, navigationIcon = { BackButton(onClose) }, actions = {
            IconButton(onClick = { vm.toggleFavorite(item) }) { Icon(if (item.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, "Favori") }
            IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, "Düzenle") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Sil") }
        })
    }) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val t = item.type
            if (t.userLabel.isNotEmpty() && item.username.isNotBlank()) FieldCard(t.userLabel, item.username, secret = false) { copy(t.userLabel, item.username) }
            if (t.secretLabel.isNotEmpty() && item.secret.isNotBlank()) FieldCard(t.secretLabel, item.secret, secret = true) { copy(t.secretLabel, item.secret) }
            if (t.extraLabel != null && item.extra.isNotBlank()) FieldCard(t.extraLabel, item.extra, secret = false) { copy(t.extraLabel, item.extra) }
            if (t == ItemType.CARD && item.pin.isNotBlank()) FieldCard("CVV / PIN", item.pin, secret = true) { copy("CVV", item.pin) }
            if (item.notes.isNotBlank()) FieldCard("Notlar", item.notes, secret = false) { copy("Not", item.notes) }
            val upd = Instant.ofEpochMilli(item.updatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
            Text("${t.label} · son değişiklik ${TrFormat.formatDateTime(upd)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (confirmDelete) ConfirmDialog("Kayıt silinsin mi?", "\"${item.title}\" kalıcı olarak silinecek.", "Sil",
        onConfirm = { vm.delete(item); onClose() }, onDismiss = { confirmDelete = false }, destructive = true)
}

@Composable
private fun FieldCard(label: String, value: String, secret: Boolean, onCopy: () -> Unit) {
    var shown by remember { mutableStateOf(!secret) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(if (shown) value else "•".repeat(value.length.coerceIn(8, 16)), style = MaterialTheme.typography.bodyLarge,
                    fontFamily = if (secret) FontFamily.Monospace else FontFamily.Default)
            }
            if (secret) IconButton(onClick = { shown = !shown }) { Icon(if (shown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (shown) "Gizle" else "Göster") }
            IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, "$label kopyala") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItem(initial: VaultItem, onCancel: () -> Unit, onSave: (VaultItem) -> Unit) {
    var type by rememberSaveable { mutableStateOf(initial.type) }
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var user by rememberSaveable { mutableStateOf(initial.username) }
    var secret by rememberSaveable { mutableStateOf(initial.secret) }
    var extra by rememberSaveable { mutableStateOf(initial.extra) }
    var pin by rememberSaveable { mutableStateOf(initial.pin) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    BackHandler(onBack = onCancel)
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (initial.id == 0L) "Yeni kayıt" else "Kaydı düzenle") }, navigationIcon = { BackButton(onCancel) }, actions = {
            TextButton(enabled = title.isNotBlank(), modifier = Modifier.testTag("item_save"), onClick = {
                onSave(initial.copy(type = type, title = title, username = user, secret = secret, extra = extra, pin = pin, notes = notes))
            }) { Text("Kaydet") }
        })
    }) { p ->
        Column(Modifier.padding(p).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ItemType.entries.forEach { t -> FilterChip(type == t, { type = t }, label = { Text(t.label, maxLines = 1) }) }
            }
            OutlinedTextField(title, { title = it.take(120) }, label = { Text("Başlık (ör. E-posta, X Bankası)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("item_title"))
            if (type.userLabel.isNotEmpty()) OutlinedTextField(user, { user = it.take(200) }, label = { Text(type.userLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (type.secretLabel.isNotEmpty()) {
                PasswordField(secret, { secret = it.take(500) }, type.secretLabel, modifier = Modifier.testTag("item_secret"))
                if (type == ItemType.LOGIN || type == ItemType.WIFI || type == ItemType.OTHER) {
                    StrengthMeter(secret)
                    TextButton(onClick = { secret = PasswordGenerator.generate(PasswordOptions(length = 20)) }) {
                        Icon(Icons.Outlined.AutoAwesome, null); Text("  Güçlü şifre üret")
                    }
                }
            }
            if (type.extraLabel != null) OutlinedTextField(extra, { extra = it.take(300) }, label = { Text(type.extraLabel!!) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (type == ItemType.LOGIN) KeyboardType.Uri else KeyboardType.Text), modifier = Modifier.fillMaxWidth())
            if (type == ItemType.CARD) PasswordField(pin, { pin = it.filter(Char::isDigit).take(8) }, "CVV / PIN (isteğe bağlı)")
            OutlinedTextField(notes, { notes = it.take(10_000) }, label = { Text("Notlar") }, modifier = Modifier.fillMaxWidth().heightIn(min = if (type == ItemType.NOTE) 220.dp else 100.dp))
        }
    }
}

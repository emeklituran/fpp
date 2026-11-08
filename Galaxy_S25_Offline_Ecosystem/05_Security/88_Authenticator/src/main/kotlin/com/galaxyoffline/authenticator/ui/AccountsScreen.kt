package com.galaxyoffline.authenticator.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.authenticator.data.Entry
import com.galaxyoffline.core.crypto.Base32
import com.galaxyoffline.core.crypto.Totp
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.core.designsystem.components.SearchField
import com.galaxyoffline.core.security.SecureClipboard
import kotlinx.coroutines.delay

fun groupCode(c: String) = if (c.length == 6) c.substring(0, 3) + " " + c.substring(3) else if (c.length == 8) c.substring(0, 4) + " " + c.substring(4) else c

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(vm: AuthViewModel, onAdd: () -> Unit, onSettings: () -> Unit) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var deleting by remember { mutableStateOf<Entry?>(null) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis() / 1000; delay(1000 - System.currentTimeMillis() % 1000) } }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Doğrulayıcı") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Hesap ekle") },
            modifier = Modifier.testTag("fab_new")) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        val list = entries
        if (list == null) { LoadingBox(Modifier.padding(p)); return@Scaffold }
        val tr = java.util.Locale.forLanguageTag("tr-TR")
        val shown = list.filter { query.isBlank() || (it.account.issuer + " " + it.account.account).lowercase(tr).contains(query.trim().lowercase(tr)) }
        Column(Modifier.padding(p).fillMaxSize()) {
            if (list.size > 4) SearchField(query, { query = it }, "Hesap ara", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            if (list.isEmpty()) EmptyState(Icons.Outlined.VerifiedUser, "Henüz hesap yok",
                "Bir hizmetin iki adımlı doğrulama ayarlarında gösterilen QR kodu tarayın. Kodlar internetsiz, bu cihazda üretilir.",
                actionLabel = "Hesap ekle", onAction = onAdd)
            else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { e ->
                    CodeCard(e, now, onCopy = { c -> SecureClipboard.copy(context, "Doğrulama kodu", c, 30); vm.messages.post("Kod kopyalandı") },
                        onEdit = { editing = e }, onDelete = { deleting = e })
                }
            }
        }
    }
    editing?.let { e ->
        var issuer by remember { mutableStateOf(e.account.issuer) }
        var acc by remember { mutableStateOf(e.account.account) }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Hesabı düzenle") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(issuer, { issuer = it.take(60) }, label = { Text("Hizmet") }, singleLine = true)
                OutlinedTextField(acc, { acc = it.take(100) }, label = { Text("Hesap") }, singleLine = true)
            } },
            confirmButton = { TextButton(onClick = { vm.rename(e, issuer, acc); editing = null }) { Text("Kaydet") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Vazgeç") } })
    }
    deleting?.let { e ->
        ConfirmDialog("Hesap silinsin mi?", "\"${e.account.issuer.ifBlank { e.account.account }}\" silinirse bu hizmete giriş için yeni kod üretemezsiniz. " +
            "Önce hizmetteki iki adımlı doğrulamayı kapattığınızdan veya başka bir yöntem eklediğinizden emin olun.", "Sil",
            onConfirm = { vm.delete(e) }, onDismiss = { deleting = null }, destructive = true)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeCard(e: Entry, now: Long, onCopy: (String) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val a = e.account
    val secret = remember(a.secret) { runCatching { Base32.decode(a.secret) }.getOrNull() }
    val code = secret?.let { Totp.code(it, now, a.period, a.digits, a.algorithm) } ?: "------"
    val left = Totp.secondsRemaining(now, a.period)
    var menu by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onCopy(code) }, onLongClick = { menu = true }, onLongClickLabel = "Seçenekler")) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.issuer.ifBlank { a.account }, style = MaterialTheme.typography.titleSmall)
                if (a.issuer.isNotBlank()) Text(a.account, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(groupCode(code), fontSize = 34.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
                    color = if (left <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("code_${e.id}"))
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { left / a.period.toFloat() }, modifier = Modifier.size(40.dp), strokeWidth = 4.dp)
                Text("$left", style = MaterialTheme.typography.labelMedium)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Düzenle") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Sil") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; onDelete() })
                }
            }
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Seçenekler") }
        }
    }
}

package com.galaxyoffline.passwordvault.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.security.Biometrics
import com.galaxyoffline.core.security.PasswordField
import java.time.LocalDate

@Composable
fun ColumnScope.VaultSettingsSection(vm: VaultViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val s by vm.settings.collectAsStateWithLifecycle()
    val bio by vm.biometricEnabled.collectAsStateWithLifecycle()
    var changePw by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<Uri?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { pendingExport = it }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingImport = it }

    if (Biometrics.strongAvailable(context)) {
        ActionRow("Biyometrik açma", subtitle = "Parmak izi / yüz ile aç. Ana şifre her zaman çalışır.", icon = Icons.Outlined.Fingerprint,
            trailing = { Switch(bio, null) }) {
            if (bio) vm.disableBiometric() else activity?.let { a ->
                runCatching { vm.bioEncryptCipher() }.onSuccess { c ->
                    Biometrics.authenticateCipher(a, c, "Biyometrik açmayı etkinleştir", "Kimliğinizi doğrulayın", vm::enableBiometric) { vm.messages.post(it) }
                }.onFailure { vm.messages.post("Biyometrik anahtar oluşturulamadı") }
            }
        }
    }
    ActionRow("Otomatik kilit", subtitle = "Uygulama arka plandayken bu süreden sonra kilitlenir.", icon = Icons.Outlined.LockClock)
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0 to "Hemen", 30 to "30 sn", 60 to "1 dk", 300 to "5 dk").forEach { (v, l) -> FilterChip(s.autoLockSeconds == v, { vm.setAutoLock(v) }, label = { Text(l) }) }
    }
    ActionRow("Panoyu temizle", subtitle = "Kopyalanan şifreler bu süre sonunda panodan silinir.", icon = Icons.Outlined.ContentPaste)
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(15 to "15 sn", 30 to "30 sn", 60 to "1 dk", 0 to "Kapalı").forEach { (v, l) -> FilterChip(s.clipboardSeconds == v, { vm.setClipboard(v) }, label = { Text(l) }) }
    }
    ActionRow("Ana şifreyi değiştir", icon = Icons.Outlined.Key) { changePw = true }
    ActionRow("Şifreli yedek al", subtitle = "Kayıtlar ana şifrenizle şifrelenmiş .gvault dosyasına yazılır.", icon = Icons.Outlined.FileDownload) {
        exportLauncher.launch("sifre-kasasi-${LocalDate.now()}.gvault")
    }
    ActionRow("Yedekten içe aktar", subtitle = "Kayıtlar mevcut kasaya eklenir.", icon = Icons.Outlined.FileUpload) { importLauncher.launch(arrayOf("*/*")) }

    if (changePw) {
        var old by remember { mutableStateOf("") }; var new by remember { mutableStateOf("") }; var conf by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { changePw = false }, title = { Text("Ana şifreyi değiştir") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField(old, { old = it }, "Mevcut şifre"); PasswordField(new, { new = it }, "Yeni şifre"); PasswordField(conf, { conf = it }, "Yeni şifre (tekrar)")
            } },
            confirmButton = { TextButton(onClick = { vm.changePassword(old, new, conf) { changePw = false } }) { Text("Değiştir") } },
            dismissButton = { TextButton(onClick = { changePw = false }) { Text("Vazgeç") } })
    }
    PasswordPrompt(pendingExport, "Yedeği şifrele", "Güvenlik için ana şifrenizi girin. Yedek bu şifreyle açılır.", { pendingExport = null }) { uri, pw -> vm.export(uri, pw) }
    PasswordPrompt(pendingImport, "Yedek parolası", "Yedeği oluştururken kullanılan ana şifreyi girin.", { pendingImport = null }) { uri, pw -> vm.import(uri, pw) }
}

@Composable
private fun PasswordPrompt(uri: Uri?, title: String, text: String, onDismiss: () -> Unit, onOk: (Uri, String) -> Unit) {
    if (uri == null) return
    var pw by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(text); PasswordField(pw, { pw = it }, "Şifre") } },
        confirmButton = { TextButton(enabled = pw.isNotEmpty(), onClick = { onOk(uri, pw); onDismiss() }) { Text("Tamam") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } })
}

package com.galaxyoffline.authenticator.ui

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.crypto.OtpAccount
import com.galaxyoffline.core.crypto.OtpAlgorithm
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.GroupCard
import com.galaxyoffline.core.designsystem.components.SectionHeader
import com.galaxyoffline.core.scanner.QrCodes
import com.galaxyoffline.core.scanner.QrScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(vm: AuthViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var issuer by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var algorithm by rememberSaveable { mutableStateOf(OtpAlgorithm.SHA1) }
    var digits by rememberSaveable { mutableIntStateOf(6) }
    var period by rememberSaveable { mutableIntStateOf(30) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bmp = runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ -> d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
            }.getOrNull()
            val r = bmp?.let(QrCodes::decodeBitmap)
            if (r == null) vm.messages.post("Görselde QR kod bulunamadı") else vm.addUri(r.text, onDone)
        }
    }

    if (scanning) {
        Scaffold(topBar = { TopAppBar(title = { Text("QR kodu tara") }, navigationIcon = { BackButton { scanning = false } }) }) { p ->
            Box(Modifier.padding(p).fillMaxSize()) {
                QrScannerView(onResult = { r -> scanning = false; vm.addUri(r.text, onDone) }, modifier = Modifier.fillMaxSize())
            }
        }
        return
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Hesap ekle") }, navigationIcon = { BackButton(onBack) }) }, snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.padding(p).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GroupCard {
                ActionRow("QR kodu tara", subtitle = "Hizmetin ekranındaki kodu kamerayla okutun", icon = Icons.Outlined.QrCodeScanner) { scanning = true }
                ActionRow("Görselden QR oku", subtitle = "Ekran görüntüsü veya fotoğraftan", icon = Icons.Outlined.Image) {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                ActionRow("Bağlantıyı yapıştır", subtitle = "otpauth://totp/… biçimindeki bağlantı", icon = Icons.Outlined.ContentPaste) {
                    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                    val t = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                    if (t.isNullOrBlank()) vm.messages.post("Panoda bağlantı yok") else vm.addUri(t, onDone)
                }
            }
            SectionHeader("Elle gir")
            OutlinedTextField(issuer, { issuer = it.take(60) }, label = { Text("Hizmet (ör. Google, e-Devlet)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("m_issuer"))
            OutlinedTextField(account, { account = it.take(100) }, label = { Text("Hesap (e-posta / kullanıcı adı)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(secret, { secret = it.take(128) }, label = { Text("Gizli anahtar (Base32)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("m_secret"))
            Text("Gelişmiş (çoğu hizmet için değiştirmeyin)", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OtpAlgorithm.entries.forEach { a -> FilterChip(algorithm == a, { algorithm = a }, label = { Text(a.name) }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(6, 8).forEach { d -> FilterChip(digits == d, { digits = d }, label = { Text("$d basamak") }) }
                listOf(30, 60).forEach { s -> FilterChip(period == s, { period = s }, label = { Text("$s sn") }) }
            }
            Button(onClick = { vm.addManual(issuer, account, secret, OtpAccount("", "", "", algorithm, digits, period), onDone) },
                enabled = secret.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("m_add")) { Text("Ekle") }
            TextButton(onClick = onBack) { Text("Vazgeç") }
        }
    }
}

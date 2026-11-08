package com.galaxyoffline.qrcode.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.scanner.QrCodes
import com.galaxyoffline.core.scanner.QrScannerView
import com.galaxyoffline.qrcode.data.QrParser
import com.galaxyoffline.qrcode.data.QrPayload
import java.io.File
import java.time.Instant
import java.time.ZoneId

data class Shown(val text: String, val format: String)

@Composable
fun ScanTab(vm: QrViewModel, onResult: (Shown) -> Unit, paused: Boolean) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var torch by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val r = runCatching { ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { d, _, _ -> d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE } }
                .getOrNull()?.let(QrCodes::decodeBitmap)
            if (r == null) vm.messages.post("Görselde kod bulunamadı") else { vm.record(r.text, r.format); onResult(Shown(r.text, r.format)) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (!paused) QrScannerView(onResult = { r ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.record(r.text, r.format); onResult(Shown(r.text, r.format))
        }, modifier = Modifier.fillMaxSize(), torch = torch)
        Row(Modifier.align(Alignment.BottomCenter).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Outlined.Image, null); Spacer(Modifier.width(8.dp)); Text("Galeriden")
            }
            FilledTonalButton(onClick = { torch = !torch }) {
                Icon(if (torch) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn, null); Spacer(Modifier.width(8.dp)); Text(if (torch) "Feneri kapat" else "Fener")
            }
        }
    }
}

private enum class Kind(val label: String) { TEXT("Metin"), URL("Bağlantı"), WIFI("Wi-Fi"), CONTACT("Kişi") }

@Composable
fun CreateTab(vm: QrViewModel) {
    val ctx = LocalContext.current
    var kind by rememberSaveable { mutableStateOf(Kind.TEXT) }
    var a by rememberSaveable { mutableStateOf("") }
    var b by rememberSaveable { mutableStateOf("") }
    var c by rememberSaveable { mutableStateOf("") }
    var d by rememberSaveable { mutableStateOf("") }
    var security by rememberSaveable { mutableStateOf("WPA") }
    var hidden by rememberSaveable { mutableStateOf(false) }
    val payload = when (kind) {
        Kind.TEXT -> a
        Kind.URL -> if (a.isBlank()) "" else QrPayload.url(a)
        Kind.WIFI -> if (a.isBlank()) "" else QrPayload.wifi(a, b, security, hidden)
        Kind.CONTACT -> if (a.isBlank()) "" else QrPayload.contact(a, b, c, d)
    }
    val bmp = remember(payload) { if (payload.isBlank() || payload.length > 2000) null else runCatching { QrCodes.encode(payload) }.getOrNull() }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Kind.entries.forEach { k -> FilterChip(kind == k, { kind = k; a = ""; b = ""; c = ""; d = "" }, label = { Text(k.label) }) }
        }
        when (kind) {
            Kind.TEXT -> OutlinedTextField(a, { a = it.take(2000) }, label = { Text("Metin") }, modifier = Modifier.fillMaxWidth().testTag("create_text"))
            Kind.URL -> OutlinedTextField(a, { a = it.take(2000) }, label = { Text("Adres (ör. ornek.com)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Kind.WIFI -> {
                OutlinedTextField(a, { a = it.take(64) }, label = { Text("Ağ adı (SSID)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("WPA", "WEP", "Açık").forEach { s -> FilterChip(security == s, { security = s }, label = { Text(s) }) } }
                if (security != "Açık") OutlinedTextField(b, { b = it.take(64) }, label = { Text("Şifre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Gizli ağ", Modifier.weight(1f)); Switch(hidden, { hidden = it }) }
            }
            Kind.CONTACT -> {
                OutlinedTextField(a, { a = it.take(80) }, label = { Text("Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(b, { b = it.take(30) }, label = { Text("Telefon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(c, { c = it.take(80) }, label = { Text("E-posta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d, { d = it.take(80) }, label = { Text("Kurum") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        if (bmp != null) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                Image(bmp.asImageBitmap(), "Oluşturulan QR kod", Modifier.fillMaxSize().padding(16.dp).testTag("qr_image"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.messages.post(if (saveToGallery(ctx, bmp)) "Galeriye kaydedildi (Resimler/QR)" else "Kaydedilemedi") }, modifier = Modifier.weight(1f)) { Text("Galeriye kaydet") }
                Button(onClick = { share(ctx, bmp) }, modifier = Modifier.weight(1f)) { Text("Paylaş") }
            }
        } else if (payload.length > 2000) Text("İçerik çok uzun.", color = MaterialTheme.colorScheme.error)
    }
}

private fun saveToGallery(ctx: Context, bmp: Bitmap): Boolean = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "qr-${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QR")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } != null
}.getOrDefault(false)

private fun share(ctx: Context, bmp: Bitmap) {
    runCatching {
        val dir = File(ctx.cacheDir, "paylasim").apply { mkdirs() }
        val f = File(dir, "qr.png"); f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".dosyalar", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "QR kodu paylaş"))
    }
}

@Composable
fun HistoryTab(vm: QrViewModel, onOpen: (Shown) -> Unit) {
    val list by vm.history.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    if (list.isEmpty()) { EmptyState(Icons.Outlined.History, "Geçmiş boş", "Taradığınız kodlar burada listelenir. Ayarlardan kaydetmeyi kapatabilirsiniz."); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { confirm = true }) { Text("Tümünü temizle") } } }
        items(list, key = { it.id }) { e ->
            val t = Instant.ofEpochMilli(e.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
            Card(onClick = { onOpen(Shown(e.text, e.format)) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(QrParser.parse(e.text, e.format).title + " · " + e.format, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(e.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(TrFormat.relativeDay(t.toLocalDate()) + " " + TrFormat.formatTime(t.toLocalTime()), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.delete(e) }) { Icon(Icons.Outlined.Delete, "Sil") }
                }
            }
        }
    }
    if (confirm) ConfirmDialog("Geçmiş silinsin mi?", "Tüm tarama geçmişi silinecek.", "Sil", onConfirm = vm::clear, onDismiss = { confirm = false }, destructive = true)
}

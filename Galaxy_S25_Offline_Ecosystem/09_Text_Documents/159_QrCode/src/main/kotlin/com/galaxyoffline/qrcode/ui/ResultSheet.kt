package com.galaxyoffline.qrcode.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.qrcode.data.QrContent
import com.galaxyoffline.qrcode.data.QrParser

private fun Context.copy(label: String, text: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
    android.widget.Toast.makeText(this, "$label kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
}

private fun Context.launch(intent: Intent) {
    try { startActivity(intent) } catch (e: ActivityNotFoundException) {
        android.widget.Toast.makeText(this, "Bu işlemi açacak uygulama bulunamadı", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResultSheet(text: String, format: String, onDismiss: () -> Unit) {
    val c = QrParser.parse(text, format)
    val ctx = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(c.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("result_title"))
            Text(format, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (c) {
                is QrContent.Url -> {
                    SelectionContainer { Text(c.url, style = MaterialTheme.typography.bodyLarge) }
                    if (!c.secure) InfoBanner("Bu bağlantı şifrelenmemiş (http). Açmadan önce adresi kontrol edin.", isError = true, icon = Icons.Outlined.WarningAmber)
                    else InfoBanner("Açmadan önce adresin beklediğiniz site olduğunu kontrol edin. Bağlantı varsayılan tarayıcınızda açılır.")
                }
                is QrContent.Wifi -> {
                    Text("Ağ: ${c.ssid}", style = MaterialTheme.typography.bodyLarge)
                    Text("Güvenlik: ${c.security}" + if (c.hidden) " · gizli ağ" else "", style = MaterialTheme.typography.bodyMedium)
                    if (c.password.isNotEmpty()) SelectionContainer { Text("Şifre: ${c.password}", style = MaterialTheme.typography.bodyLarge) }
                }
                is QrContent.Contact -> {
                    Text(c.name.ifBlank { "(isimsiz)" }, style = MaterialTheme.typography.bodyLarge)
                    (c.phones + c.emails + listOfNotNull(c.org.ifBlank { null })).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
                is QrContent.Geo -> Text("Enlem ${c.lat}, boylam ${c.lon}", style = MaterialTheme.typography.bodyLarge)
                is QrContent.Email -> Text(listOf(c.to, c.subject).filter { it.isNotBlank() }.joinToString("\n"), style = MaterialTheme.typography.bodyLarge)
                is QrContent.Sms -> Text("${c.number}\n${c.body}", style = MaterialTheme.typography.bodyLarge)
                is QrContent.Phone -> Text(c.number, style = MaterialTheme.typography.bodyLarge)
                else -> SelectionContainer { Text(c.raw, style = MaterialTheme.typography.bodyLarge) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (c) {
                    is QrContent.Url -> FilledTonalButton(onClick = { ctx.launch(Intent(Intent.ACTION_VIEW, Uri.parse(c.url))) }) { Text("Tarayıcıda aç") }
                    is QrContent.Wifi -> {
                        if (c.password.isNotEmpty()) FilledTonalButton(onClick = { ctx.copy("Wi-Fi şifresi", c.password) }) { Text("Şifreyi kopyala") }
                        AssistChip(onClick = { ctx.launch(Intent(Settings.Panel.ACTION_WIFI)) }, label = { Text("Wi-Fi ayarları") })
                    }
                    is QrContent.Contact -> FilledTonalButton(onClick = {
                        ctx.launch(Intent(ContactsContract.Intents.Insert.ACTION).setType(ContactsContract.RawContacts.CONTENT_TYPE)
                            .putExtra(ContactsContract.Intents.Insert.NAME, c.name).putExtra(ContactsContract.Intents.Insert.PHONE, c.phones.firstOrNull())
                            .putExtra(ContactsContract.Intents.Insert.EMAIL, c.emails.firstOrNull()).putExtra(ContactsContract.Intents.Insert.COMPANY, c.org))
                    }) { Text("Kişilere ekle") }
                    is QrContent.Phone -> FilledTonalButton(onClick = { ctx.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.number}"))) }) { Text("Ara") }
                    is QrContent.Sms -> FilledTonalButton(onClick = { ctx.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${c.number}")).putExtra("sms_body", c.body)) }) { Text("SMS yaz") }
                    is QrContent.Email -> FilledTonalButton(onClick = {
                        ctx.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${c.to}")).putExtra(Intent.EXTRA_SUBJECT, c.subject).putExtra(Intent.EXTRA_TEXT, c.body))
                    }) { Text("E-posta yaz") }
                    is QrContent.Geo -> FilledTonalButton(onClick = { ctx.launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${c.lat},${c.lon}?q=${c.lat},${c.lon}"))) }) { Text("Haritada aç") }
                    else -> Unit
                }
                AssistChip(onClick = { ctx.copy("İçerik", c.raw) }, label = { Text("Kopyala") })
                AssistChip(onClick = { ctx.launch(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, c.raw), "Paylaş")) },
                    label = { Text("Paylaş") })
            }
        }
    }
}

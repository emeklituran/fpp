package com.galaxyoffline.authenticator

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galaxyoffline.authenticator.data.LockState
import com.galaxyoffline.authenticator.ui.AccountsScreen
import com.galaxyoffline.authenticator.ui.AddScreen
import com.galaxyoffline.authenticator.ui.AuthViewModel
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.PermissionUse
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.components.IconBadge
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.core.security.Biometrics
import com.galaxyoffline.core.security.PasswordField
import com.galaxyoffline.core.security.applySecureWindow
import java.time.LocalDate

val Accent = AppAccent(primaryLight = Color(0xFF0D47A1), primaryDark = Color(0xFFA6C8FF))

val Info = AppInfo(
    name = "Doğrulayıcı",
    description = "RFC 6238 uyumlu, tamamen çevrimdışı iki adımlı doğrulama (TOTP) kod üretici.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.VerifiedUser,
    permissions = listOf(
        PermissionUse("Kamera", "İsteğe bağlı: hesap eklerken QR kodu taramak için. Görüntü kaydedilmez."),
        PermissionUse("Biyometri", "İsteğe bağlı uygulama kilidi için."),
    ),
    limitations = listOf(
        "Kodlar telefon saatine dayanır; saat yanlışsa kodlar reddedilir (Ayarlar > Genel yönetim > Tarih ve saat > Otomatik).",
        "Sayaç tabanlı (HOTP) hesaplar desteklenmez.",
        "Telefonu kaybederseniz hesaplara erişim için şifreli yedeğiniz veya hizmetin kurtarma kodları gerekir.",
    ),
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySecureWindow()
        setGalaxyContent(Accent) { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    val vm: AuthViewModel = viewModel()
    val locked by LockState.locked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (locked) {
        var err by remember { mutableStateOf<String?>(null) }
        val prompt = { (context as? FragmentActivity)?.let { a -> Biometrics.authenticateDevice(a, "Doğrulayıcı", LockState::unlock) { err = it } } ?: Unit }
        LaunchedEffect(Unit) { prompt() }
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                IconBadge(Icons.Outlined.Lock, size = 80.dp)
                Text("Doğrulayıcı kilitli", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = { prompt() }) { Text("Kilidi aç") }
            }
        }
        return
    }
    val nav = rememberNavController()
    NavHost(nav, startDestination = "hesaplar") {
        composable("hesaplar") {
            AccountsScreen(vm, onAdd = { nav.navigate("ekle") { launchSingleTop = true } },
                onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } })
        }
        composable("ekle") { AddScreen(vm, onDone = nav::safeBack, onBack = nav::safeBack) }
        galaxyStandardScreens(nav, Info) {
            val lock by vm.lockEnabled.collectAsStateWithLifecycle()
            var exportUri by remember { mutableStateOf<Uri?>(null) }
            var importUri by remember { mutableStateOf<Uri?>(null) }
            val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { exportUri = it }
            val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri = it }
            if (Biometrics.anyAvailable(context)) ActionRow("Uygulama kilidi", subtitle = "Açılışta biyometri veya ekran kilidi iste", icon = Icons.Outlined.Lock,
                trailing = { Switch(lock, null) }) { vm.setLock(!lock) }
            ActionRow("Şifreli yedek al", subtitle = "Hesaplar belirlediğiniz parolayla şifrelenir", icon = Icons.Outlined.FileDownload) {
                exp.launch("dogrulayici-${LocalDate.now()}.gauth")
            }
            ActionRow("Yedekten içe aktar", icon = Icons.Outlined.FileUpload) { imp.launch(arrayOf("*/*")) }
            exportUri?.let { u -> PasswordDialog("Yedek parolası", "En az 8 karakter. Bu parola olmadan yedek açılamaz.", { exportUri = null }) { vm.export(u, it) } }
            importUri?.let { u -> PasswordDialog("Yedek parolası", "Yedeği oluştururken belirlediğiniz parola.", { importUri = null }) { vm.import(u, it) } }
        }
    }
}

@Composable
private fun PasswordDialog(title: String, text: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(text); PasswordField(pw, { pw = it }, "Parola") } },
        confirmButton = { TextButton(enabled = pw.isNotEmpty(), onClick = { onOk(pw); onDismiss() }) { Text("Tamam") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } })
}

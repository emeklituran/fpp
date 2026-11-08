package com.galaxyoffline.passwordvault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.components.IconBadge
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.security.PasswordField
import com.galaxyoffline.core.security.StrengthMeter

@Composable
fun SetupScreen(vm: VaultViewModel) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var pw by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var understood by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(Icons.Outlined.Shield, size = 80.dp)
            Text("Şifre Kasası", style = MaterialTheme.typography.headlineSmall)
            Text("Kasanızı korumak için bir ana şifre belirleyin. Tüm kayıtlar bu şifreden türetilen anahtarla AES-256 ile şifrelenir.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PasswordField(pw, { pw = it }, "Ana şifre (en az 8 karakter)", imeAction = ImeAction.Next, modifier = Modifier.testTag("setup_pw"))
            StrengthMeter(pw)
            PasswordField(confirm, { confirm = it }, "Ana şifre (tekrar)", isError = confirm.isNotEmpty() && confirm != pw,
                supporting = if (confirm.isNotEmpty() && confirm != pw) "Şifreler eşleşmiyor" else null, modifier = Modifier.testTag("setup_confirm"))
            InfoBanner("Ana şifrenizi unutursanız verileriniz kurtarılamaz. Uygulamada arka kapı veya sıfırlama yoktur; bir yere güvenle not edin.",
                isError = true, icon = Icons.Outlined.WarningAmber)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(understood, { understood = it }, modifier = Modifier.testTag("setup_ok"))
                Text("Anladım, ana şifremi unutursam verilerimi kaybederim.", style = MaterialTheme.typography.bodyMedium)
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = { vm.setup(pw, confirm) }, enabled = understood && pw.length >= 8 && pw == confirm && !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("setup_create")) {
                if (busy) CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.dp) else Text("Kasayı oluştur")
            }
        }
    }
}

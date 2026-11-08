package com.galaxyoffline.core.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.crypto.PasswordStrength
import com.galaxyoffline.core.designsystem.components.IconBadge

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supporting: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }, onNext = { onDone() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (visible) "Gizle" else "Göster")
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun StrengthMeter(password: String, modifier: Modifier = Modifier) {
    if (password.isEmpty()) return
    val s = remember(password) { PasswordStrength.evaluate(password) }
    val color = when (s.score) { 0, 1 -> MaterialTheme.colorScheme.error; 2 -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }
    Column(modifier.fillMaxWidth()) {
        LinearProgressIndicator(progress = { (s.score + 1) / 5f }, color = color, modifier = Modifier.fillMaxWidth())
        Text("${s.label} · yaklaşık ${s.bits.toInt()} bit" + if (s.warnings.isNotEmpty()) " · " + s.warnings.joinToString(", ") else "",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Ortak kilit ekranı. Çok sayıda hatalı denemede kısa süreli bekleme uygulanır. */
@Composable
fun LockScreen(
    appName: String,
    busy: Boolean,
    error: String?,
    lockedOutSeconds: Int,
    biometricAvailable: Boolean,
    onUnlock: (String) -> Unit,
    onBiometric: () -> Unit,
) {
    var pw by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            IconBadge(Icons.Outlined.Lock, size = 80.dp)
            Spacer(Modifier.height(16.dp))
            Text(appName, style = MaterialTheme.typography.headlineSmall)
            Text("Kilitli", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            PasswordField(pw, { pw = it }, "Ana şifre", isError = error != null, supporting = error ?: if (lockedOutSeconds > 0) "$lockedOutSeconds sn bekleyin" else null,
                onDone = { if (pw.isNotEmpty() && !busy && lockedOutSeconds == 0) onUnlock(pw) }, modifier = Modifier.testTag("master_password"))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onUnlock(pw) }, enabled = pw.isNotEmpty() && !busy && lockedOutSeconds == 0,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("unlock")) {
                if (busy) CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.dp) else Text("Kilidi aç")
            }
            if (biometricAvailable) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBiometric, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Outlined.Fingerprint, null); Spacer(Modifier.padding(4.dp)); Text("Biyometri ile aç")
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Verileriniz bu cihazda AES-256 ile şifrelenmiştir. Ana şifrenizi unutursanız veriler kurtarılamaz.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

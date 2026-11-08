package com.galaxyoffline.alarm.ring

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.alarm.Accent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent

/** Kilit ekranında da görünen tam ekran alarm. Geri tuşu alarmı kapatmaz (yanlışlıkla susturmayı önler). */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setGalaxyContent(Accent) {
            val info by AlarmRingService.ringing.collectAsStateWithLifecycle()
            LaunchedEffect(info) { if (info == null) finish() }
            BackHandler { /* bilinçli olarak boş */ }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(48.dp))
                        Icon(Icons.Outlined.Alarm, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        val i = info
                        Text(if (i != null) "%02d:%02d".format(i.hour, i.minute) else "", fontSize = 88.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(i?.label?.ifBlank { "Alarm" } ?: "Alarm", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
                        if (i?.preview == true) Text("Önizleme", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FilledTonalButton(onClick = { AlarmRingService.command(this@RingActivity, AlarmRingService.ACTION_SNOOZE) },
                            modifier = Modifier.fillMaxWidth().height(64.dp).testTag("snooze")) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Snooze, null); Spacer(Modifier.padding(4.dp))
                                Text("Ertele (${info?.snoozeMinutes ?: 10} dk)", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Button(onClick = { AlarmRingService.command(this@RingActivity, AlarmRingService.ACTION_DISMISS) },
                            modifier = Modifier.fillMaxWidth().height(72.dp).testTag("dismiss")) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AlarmOff, null); Spacer(Modifier.padding(4.dp))
                                Text("Kapat", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.galaxyoffline.timer.ring

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TimerOff
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.timer.Accent
import com.galaxyoffline.timer.data.Phase
import com.galaxyoffline.timer.data.TimerMode
import com.galaxyoffline.timer.data.TimerStore

/** Kilit ekranında da görünen "süre doldu" ekranı. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true); setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        TimerStore.load(this)
        setGalaxyContent(Accent) {
            val ringing by RingService.ringing.collectAsStateWithLifecycle()
            val s by TimerStore.state.collectAsStateWithLifecycle()
            LaunchedEffect(ringing) { if (!ringing) finish() }
            BackHandler { }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(64.dp))
                        Icon(Icons.Outlined.TimerOff, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(if (s.mode == TimerMode.POMODORO) "${s.phase.label} bitti" else "Süre doldu", style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
                        if (s.label.isNotBlank() && s.mode == TimerMode.TIMER) Text(s.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (s.mode == TimerMode.POMODORO) FilledTonalButton(onClick = { RingService.command(this@RingActivity, RingService.ACTION_NEXT) },
                            modifier = Modifier.fillMaxWidth().height(64.dp)) { Text(if (s.phase == Phase.WORK) "Molayı başlat" else "Çalışmaya başla", style = MaterialTheme.typography.titleMedium) }
                        else FilledTonalButton(onClick = { RingService.command(this@RingActivity, RingService.ACTION_PLUS) }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                            Text("+1 dakika", style = MaterialTheme.typography.titleMedium)
                        }
                        Button(onClick = { RingService.command(this@RingActivity, RingService.ACTION_STOP) }, modifier = Modifier.fillMaxWidth().height(72.dp).testTag("ring_stop")) {
                            Text("Kapat", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

package com.galaxyoffline.core.designsystem.theme

import android.graphics.Color as AColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Tüm uygulamalarda ortak giriş: kenardan kenara görünüm, kayıtlı tema ve sistem çubuğu renkleri.
 * Ayarlar okunana kadar (milisaniyeler) pencere arka planı görünür; yanlış tema yanıp sönmez.
 */
fun ComponentActivity.setGalaxyContent(accent: AppAccent, content: @Composable () -> Unit) {
    enableEdgeToEdge()
    val prefs = GalaxyPreferences.get(this)
    setContent {
        val settings by prefs.theme.collectAsStateWithLifecycle(initialValue = null)
        val current = settings ?: return@setContent
        val dark = current.mode.isDark(isSystemInDarkTheme())
        DisposableEffect(dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(AColor.TRANSPARENT, AColor.TRANSPARENT) { dark },
                navigationBarStyle = SystemBarStyle.auto(AColor.TRANSPARENT, AColor.TRANSPARENT) { dark },
            )
            onDispose {}
        }
        GalaxyTheme(accent = accent, mode = current.mode, dynamicColor = current.dynamicColor, content = content)
    }
}

package com.galaxyoffline.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
    SYSTEM("Sistem ayarı"),
    LIGHT("Açık"),
    DARK("Koyu"),
    AMOLED("AMOLED gerçek siyah");

    fun isDark(systemDark: Boolean) = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK, AMOLED -> true
    }

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.name == key } ?: SYSTEM
    }
}

/**
 * Her uygulamanın kendine özgü vurgu rengi. Ailenin geri kalanı (yüzeyler, tipografi, şekiller) ortaktır;
 * böylece uygulamalar aynı aileye ait görünür ama birbirinin kopyası olmaz.
 */
@Immutable
data class AppAccent(
    val primaryLight: Color,
    val primaryDark: Color,
    val tertiaryLight: Color = lerp(primaryLight, Color(0xFF7A5900), 0.55f),
    val tertiaryDark: Color = lerp(primaryDark, Color(0xFFFFD580), 0.55f),
)

private fun lightScheme(a: AppAccent): ColorScheme {
    val secondary = lerp(a.primaryLight, Color(0xFF5F6368), 0.55f)
    return lightColorScheme(
        primary = a.primaryLight,
        onPrimary = Color.White,
        primaryContainer = lerp(a.primaryLight, Color.White, 0.82f),
        onPrimaryContainer = lerp(a.primaryLight, Color.Black, 0.65f),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = lerp(secondary, Color.White, 0.82f),
        onSecondaryContainer = lerp(secondary, Color.Black, 0.65f),
        tertiary = a.tertiaryLight,
        onTertiary = Color.White,
        tertiaryContainer = lerp(a.tertiaryLight, Color.White, 0.82f),
        onTertiaryContainer = lerp(a.tertiaryLight, Color.Black, 0.65f),
        background = Color(0xFFF8F9FB),
        onBackground = Color(0xFF191C20),
        surface = Color(0xFFF8F9FB),
        onSurface = Color(0xFF191C20),
        surfaceVariant = Color(0xFFE0E3E8),
        onSurfaceVariant = Color(0xFF43474E),
        outline = Color(0xFF73777F),
        outlineVariant = Color(0xFFC3C7CF),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF2F3F6),
        surfaceContainer = Color(0xFFECEEF1),
        surfaceContainerHigh = Color(0xFFE6E8EB),
        surfaceContainerHighest = Color(0xFFE0E2E5),
    )
}

private fun darkScheme(a: AppAccent, amoled: Boolean): ColorScheme {
    val secondary = lerp(a.primaryDark, Color(0xFFBFC6D0), 0.55f)
    val base = darkColorScheme(
        primary = a.primaryDark,
        onPrimary = lerp(a.primaryDark, Color.Black, 0.78f),
        primaryContainer = lerp(a.primaryDark, Color.Black, 0.62f),
        onPrimaryContainer = lerp(a.primaryDark, Color.White, 0.72f),
        secondary = secondary,
        onSecondary = lerp(secondary, Color.Black, 0.78f),
        secondaryContainer = lerp(secondary, Color.Black, 0.65f),
        onSecondaryContainer = lerp(secondary, Color.White, 0.72f),
        tertiary = a.tertiaryDark,
        onTertiary = lerp(a.tertiaryDark, Color.Black, 0.78f),
        tertiaryContainer = lerp(a.tertiaryDark, Color.Black, 0.62f),
        onTertiaryContainer = lerp(a.tertiaryDark, Color.White, 0.72f),
        background = Color(0xFF111318),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF111318),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF43474E),
        onSurfaceVariant = Color(0xFFC3C7CF),
        outline = Color(0xFF8D9199),
        outlineVariant = Color(0xFF43474E),
        surfaceContainerLowest = Color(0xFF0C0E12),
        surfaceContainerLow = Color(0xFF191C20),
        surfaceContainer = Color(0xFF1D2024),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33353A),
    )
    return if (amoled) base.toAmoled() else base
}

/** AMOLED: arka plan gerçek siyah (#000000), kartlar ise ayırt edilebilir koyu gri tonlarında. */
fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceVariant = Color(0xFF1E1E1E),
)

private val GalaxyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val GalaxyTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun GalaxyTheme(
    accent: AppAccent,
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark(isSystemInDarkTheme())
    val amoled = mode == ThemeMode.AMOLED
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && dark -> dynamicDarkColorScheme(context).let { if (amoled) it.toAmoled() else it }
        dynamicColor -> dynamicLightColorScheme(context)
        dark -> darkScheme(accent, amoled)
        else -> lightScheme(accent)
    }
    MaterialTheme(colorScheme = scheme, typography = GalaxyTypography, shapes = GalaxyShapes, content = content)
}

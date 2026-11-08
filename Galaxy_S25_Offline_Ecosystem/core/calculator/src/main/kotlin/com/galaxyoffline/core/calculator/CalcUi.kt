package com.galaxyoffline.core.calculator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxyoffline.core.math.CalcFormat

enum class KeyStyle { Number, Operator, Function, Accent, Danger }

/** Ekran okuyucu için sembollerin Türkçe adları. */
fun spokenLabel(label: String): String = when (label) {
    "÷" -> "bölü"; "×" -> "çarpı"; "−" -> "eksi"; "+" -> "artı"; "=" -> "eşittir"; "%" -> "yüzde"
    "⌫" -> "sil"; "AC" -> "tümünü temizle"; "( )" -> "parantez"; "±" -> "işaret değiştir"; "," -> "virgül"
    "√" -> "karekök"; "π" -> "pi"; "x!" -> "faktöriyel"; "xʸ" -> "üs"; "x²" -> "kare"
    else -> label
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.CalcKey(
    label: String,
    style: KeyStyle,
    haptics: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    onLongClick: (() -> Unit)? = null,
    small: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = when (style) {
        KeyStyle.Number -> c.surfaceContainerHigh to c.onSurface
        KeyStyle.Operator -> c.secondaryContainer to c.onSecondaryContainer
        KeyStyle.Function -> c.surfaceContainer to c.onSurfaceVariant
        KeyStyle.Accent -> c.primary to c.onPrimary
        KeyStyle.Danger -> c.tertiaryContainer to c.onTertiaryContainer
    }
    val haptic = LocalHapticFeedback.current
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(if (small) 16.dp else 24.dp),
        modifier = modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(4.dp)
            .testTag("key_$label")
            .semantics { contentDescription = spokenLabel(label) }
            .combinedClickable(
                role = Role.Button,
                onClick = {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = if (small) 17.sp else 26.sp,
                fontWeight = if (style == KeyStyle.Number) FontWeight.Normal else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ColumnScope.KeyRow(weight: Float = 1f, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().weight(weight), content = content)
}

/** İfade, canlı önizleme / hata ve bellek göstergesi. Sonuç uzun basılınca kopyalanır. */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("DEPRECATION")
@Composable
fun CalcDisplay(state: CalcUiState, modifier: Modifier = Modifier, header: (@Composable RowScope.() -> Unit)? = null) {
    val clipboard = LocalClipboardManager.current
    val shown = CalcFormat.expression(state.expression, state.grouping).ifEmpty { "0" }
    val size = when {
        shown.length > 30 -> 26.sp
        shown.length > 18 -> 34.sp
        shown.length > 11 -> 44.sp
        else -> 60.sp
    }
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            header?.invoke(this)
            Box(Modifier.weight(1f))
            if (state.memory != null) {
                Text("M", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { contentDescription = "Bellekte değer var" })
            }
        }
        Text(
            shown,
            fontSize = size,
            lineHeight = size * 1.1f,
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.StartEllipsis,
            color = if (state.justEvaluated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("display_expression")
                .combinedClickable(onClick = {}, onLongClick = {
                    clipboard.setText(AnnotatedString(shown))
                }),
        )
        val secondary = state.error ?: state.preview?.let { "= $it" } ?: ""
        Text(
            secondary,
            style = MaterialTheme.typography.headlineSmall,
            color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("display_preview")
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}


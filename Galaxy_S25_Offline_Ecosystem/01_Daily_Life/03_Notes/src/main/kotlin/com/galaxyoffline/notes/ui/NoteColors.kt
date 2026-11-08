package com.galaxyoffline.notes.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Not renkleri: (ad, açık tema tonu, koyu tema tonu). 0 = varsayılan yüzey. */
val NoteColors = listOf(
    Triple("Varsayılan", Color.Unspecified, Color.Unspecified),
    Triple("Kırmızı", Color(0xFFFFD6D1), Color(0xFF5C1E19)),
    Triple("Turuncu", Color(0xFFFFE2C2), Color(0xFF5A3410)),
    Triple("Sarı", Color(0xFFFFF3B0), Color(0xFF524712)),
    Triple("Yeşil", Color(0xFFD4F2D0), Color(0xFF1E4620)),
    Triple("Turkuaz", Color(0xFFC8F0EC), Color(0xFF14403C)),
    Triple("Mavi", Color(0xFFD3E4FF), Color(0xFF1B3557)),
    Triple("Mor", Color(0xFFEADCFF), Color(0xFF3B2659)),
)

@Composable
fun noteColor(index: Int): Color {
    val entry = NoteColors.getOrElse(index) { NoteColors[0] }
    val dark = MaterialTheme.colorScheme.surface.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val base = if (dark) entry.third else entry.second
    return if (base == Color.Unspecified) MaterialTheme.colorScheme.surfaceContainerHigh
    else lerp(base, MaterialTheme.colorScheme.surface, 0.1f)
}

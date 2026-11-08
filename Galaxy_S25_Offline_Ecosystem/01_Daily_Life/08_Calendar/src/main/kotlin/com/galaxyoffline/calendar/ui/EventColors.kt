package com.galaxyoffline.calendar.ui

import androidx.compose.ui.graphics.Color

val EventColors = listOf(
    "Kırmızı" to Color(0xFFE53935), "Mavi" to Color(0xFF1E88E5), "Yeşil" to Color(0xFF43A047), "Turuncu" to Color(0xFFFB8C00),
    "Mor" to Color(0xFF8E24AA), "Turkuaz" to Color(0xFF00ACC1), "Pembe" to Color(0xFFD81B60), "Gri" to Color(0xFF757575),
)

fun eventColor(i: Int) = EventColors.getOrElse(i) { EventColors[0] }.second

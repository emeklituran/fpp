package com.fpp.finans.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Türkçe para/yüzde biçimlendirme ve kullanıcı girişini güvenli ayrıştırma. */
object Money {
    val LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    private val symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance(LOCALE)
    private val thousandsPattern = Regex("^\\d{1,3}(\\.\\d{3})+$")

    /** 1234.5 -> "1.234,50 ₺" */
    fun format(value: Double, withSymbol: Boolean = true): String {
        val v = if (!value.isFinite() || abs(value) < 0.005) 0.0 else value
        val text = DecimalFormat("#,##0.00", symbols).format(v)
        return if (withSymbol) "$text ₺" else text
    }

    /** Grafik ekseni için kısa biçim: 1.250.000 -> "1,25 mn", 12.500 -> "12,5 bin". */
    fun formatCompact(value: Double): String {
        val v = if (value.isFinite()) value else 0.0
        val fmt = DecimalFormat("#,##0.##", symbols)
        return when {
            abs(v) >= 1_000_000 -> fmt.format(v / 1_000_000) + " mn"
            abs(v) >= 1_000 -> DecimalFormat("#,##0.#", symbols).format(v / 1_000) + " bin"
            else -> DecimalFormat("#,##0", symbols).format(v)
        }
    }

    /** Düzenleme alanlarına ön doldurma için: 1500.5 -> "1500,5" */
    fun formatInput(value: Double): String {
        if (!value.isFinite() || value == 0.0) return ""
        return DecimalFormat("0.##", symbols).format(value)
    }

    /** 40.0 -> "%40", 2.5 -> "%2,5" (Türkçede yüzde işareti başa yazılır). */
    fun formatPercent(value: Double): String =
        "%" + DecimalFormat("#,##0.##", symbols).format(if (value.isFinite()) value else 0.0)

    /**
     * Kullanıcı girişini sayıya çevirir. Kabul edilenler: "1500", "1500,75", "1.500", "1.500,75", "1500.75",
     * "1,500.75", "₺ 2.000". Geçersiz girişte null döner (asla istisna fırlatmaz).
     */
    fun parse(input: String?): Double? {
        if (input == null) return null
        val s = input.trim()
            .replace("₺", "")
            .replace("TL", "", ignoreCase = true)
            .replace("%", "")
            .replace(" ", "")
            .replace(" ", "")
        if (s.isEmpty()) return null
        val hasComma = ',' in s
        val hasDot = '.' in s
        val normalized = when {
            hasComma && hasDot ->
                if (s.lastIndexOf(',') > s.lastIndexOf('.')) s.replace(".", "").replace(',', '.')
                else s.replace(",", "")
            hasComma -> if (s.count { it == ',' } > 1) s.replace(",", "") else s.replace(',', '.')
            hasDot -> when {
                thousandsPattern.matches(s) -> s.replace(".", "")
                s.count { it == '.' } > 1 -> return null
                else -> s
            }
            else -> s
        }
        if (!normalized.all { it.isDigit() || it == '.' || it == '-' }) return null
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value.isFinite()) value else null
    }
}

object Dates {
    val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Money.LOCALE)
    val shortMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yy", Money.LOCALE)
    val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Money.LOCALE)

    fun formatMonthYear(value: YearMonth): String =
        value.format(monthYear).replaceFirstChar { it.titlecase(Money.LOCALE) }

    /** 27 ay -> "2 yıl 3 ay" */
    fun formatDuration(months: Int): String {
        val y = months / 12
        val m = months % 12
        return when {
            y == 0 -> "$m ay"
            m == 0 -> "$y yıl"
            else -> "$y yıl $m ay"
        }
    }
}

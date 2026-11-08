package com.galaxyoffline.core.common

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Türkçe sayı, tarih ve süre biçimlendirme yardımcıları (tüm uygulamalarda ortak). */
object TrFormat {
    val LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    private val symbols = DecimalFormatSymbols.getInstance(LOCALE)
    private val thousands = Regex("^-?\\d{1,3}(\\.\\d{3})+$")

    val date: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE)
    val dateShort: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", LOCALE)
    val dateWithDay: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", LOCALE)
    val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE)
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE)
    val dateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", LOCALE)
    val dayName: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", LOCALE)

    fun capitalize(text: String): String = text.replaceFirstChar { it.titlecase(LOCALE) }

    fun formatDate(d: LocalDate): String = d.format(date)
    fun formatDateWithDay(d: LocalDate): String = d.format(dateWithDay)
    fun formatMonthYear(d: LocalDate): String = capitalize(d.format(monthYear))
    fun formatTime(t: LocalTime): String = t.format(time)
    fun formatDateTime(dt: LocalDateTime): String = dt.format(dateTime)

    /** "Bugün", "Yarın", "Dün" veya tarih. */
    fun relativeDay(d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
        today -> "Bugün"
        today.plusDays(1) -> "Yarın"
        today.minusDays(1) -> "Dün"
        else -> if (d.year == today.year) d.format(dateShort) else formatDate(d)
    }

    /** 1234.5 -> "1.234,5" (en fazla [maxDecimals] ondalık). */
    fun number(value: Double, maxDecimals: Int = 2, grouping: Boolean = true): String {
        if (!value.isFinite()) return "—"
        val pattern = (if (grouping) "#,##0" else "0") + if (maxDecimals > 0) "." + "#".repeat(maxDecimals) else ""
        val v = if (abs(value) < Math.pow(10.0, -(maxDecimals + 1).toDouble())) 0.0 else value
        return DecimalFormat(pattern, symbols).apply { roundingMode = RoundingMode.HALF_UP }.format(v)
    }

    fun money(value: Double): String =
        DecimalFormat("#,##0.00", symbols).format(if (abs(value) < 0.005) 0.0 else value) + " ₺"

    /** Kullanıcı girişini sayıya çevirir: "1.500,75", "1500.75", "1,5" kabul edilir. Geçersizse null. */
    fun parseDecimal(input: String?): Double? {
        val s = input?.trim()?.replace(" ", "")?.replace(" ", "")?.replace("₺", "") ?: return null
        if (s.isEmpty()) return null
        val normalized = when {
            ',' in s && '.' in s ->
                if (s.lastIndexOf(',') > s.lastIndexOf('.')) s.replace(".", "").replace(',', '.') else s.replace(",", "")
            ',' in s -> if (s.count { it == ',' } > 1) return null else s.replace(',', '.')
            '.' in s -> when {
                thousands.matches(s) -> s.replace(".", "")
                s.count { it == '.' } > 1 -> return null
                else -> s
            }
            else -> s
        }
        return normalized.toBigDecimalOrNull()?.toDouble()?.takeIf { it.isFinite() }
    }

    /** Süreyi "01:02:03,45" biçiminde verir (saat yoksa "02:03,45"). */
    fun stopwatch(millis: Long): String {
        val ms = millis.coerceAtLeast(0)
        val h = ms / 3_600_000
        val m = (ms / 60_000) % 60
        val s = (ms / 1000) % 60
        val cs = (ms / 10) % 100
        return if (h > 0) "%d:%02d:%02d,%02d".format(h, m, s, cs) else "%02d:%02d,%02d".format(m, s, cs)
    }

    /** "2 saat 5 dakika" gibi okunabilir süre. */
    fun humanDuration(d: Duration): String {
        val total = d.toMinutes().coerceAtLeast(0)
        val days = total / (60 * 24)
        val hours = (total / 60) % 24
        val minutes = total % 60
        val parts = buildList {
            if (days > 0) add("$days gün")
            if (hours > 0) add("$hours saat")
            if (minutes > 0 || isEmpty()) add("$minutes dakika")
        }
        return parts.joinToString(" ")
    }

    fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

}

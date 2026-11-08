package com.galaxyoffline.core.crypto

import java.security.SecureRandom
import kotlin.math.ln

data class PasswordOptions(
    val length: Int = 20,
    val upper: Boolean = true,
    val lower: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
    val excludeAmbiguous: Boolean = true,
)

/** Kriptografik olarak güvenli rastgele şifre üretici (SecureRandom). */
object PasswordGenerator {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/~"
    private const val AMBIGUOUS = "Il1O0o|`'\""
    private val random = SecureRandom()

    fun pools(o: PasswordOptions): List<String> = buildList {
        if (o.upper) add(UPPER); if (o.lower) add(LOWER); if (o.digits) add(DIGITS); if (o.symbols) add(SYMBOLS)
    }.map { p -> if (o.excludeAmbiguous) p.filterNot { it in AMBIGUOUS } else p }

    fun generate(o: PasswordOptions): String {
        val pools = pools(o)
        require(pools.isNotEmpty()) { "En az bir karakter türü seçin" }
        val length = o.length.coerceIn(maxOf(4, pools.size), 128)
        val all = pools.joinToString("")
        // Her seçili türden en az bir karakter, sonra karıştırma (Fisher–Yates).
        val chars = pools.map { it[random.nextInt(it.length)] }.toMutableList()
        while (chars.size < length) chars += all[random.nextInt(all.length)]
        for (i in chars.indices.reversed()) { val j = random.nextInt(i + 1); val t = chars[i]; chars[i] = chars[j]; chars[j] = t }
        return chars.joinToString("")
    }

    fun entropyBits(o: PasswordOptions): Double {
        val n = pools(o).sumOf { it.length }
        return if (n == 0) 0.0 else o.length.coerceIn(1, 128) * ln(n.toDouble()) / ln(2.0)
    }
}

data class Strength(val score: Int, val label: String, val bits: Double, val warnings: List<String>)

/** Şifre gücü tahmini: karakter çeşitliliği, uzunluk, tekrar ve sıralı desenler, yaygın şifreler. */
object PasswordStrength {
    private val common = setOf("123456", "123456789", "12345678", "password", "qwerty", "111111", "123123", "abc123",
        "sifre", "şifre", "parola", "1234567890", "000000", "iloveyou", "admin", "galatasaray", "fenerbahce", "besiktas",
        "trabzonspor", "qwerty123", "asdasd", "1q2w3e4r", "password1", "123321", "654321")

    fun evaluate(pw: String): Strength {
        if (pw.isEmpty()) return Strength(0, "Boş", 0.0, emptyList())
        val warnings = mutableListOf<String>()
        var pool = 0
        if (pw.any { it.isLowerCase() }) pool += 26
        if (pw.any { it.isUpperCase() }) pool += 26
        if (pw.any { it.isDigit() }) pool += 10
        if (pw.any { !it.isLetterOrDigit() }) pool += 33
        var bits = pw.length * ln(pool.coerceAtLeast(1).toDouble()) / ln(2.0)
        val lower = pw.lowercase()
        if (lower in common || common.any { lower.contains(it) && it.length >= 6 }) { bits = minOf(bits, 10.0); warnings += "Çok yaygın bir şifre veya parçası" }
        if (Regex("(.)\\1{2,}").containsMatchIn(pw)) { bits -= 10; warnings += "Tekrarlanan karakterler" }
        if (hasSequence(lower)) { bits -= 10; warnings += "Sıralı karakterler (abc, 123)" }
        if (Regex("(19|20)\\d{2}").containsMatchIn(pw)) warnings += "Yıl içeriyor (tahmin edilebilir)"
        if (pw.length < 12) warnings += "12 karakterden kısa"
        bits = bits.coerceAtLeast(0.0)
        val score = when { bits < 28 -> 0; bits < 40 -> 1; bits < 60 -> 2; bits < 80 -> 3; else -> 4 }
        val label = listOf("Çok zayıf", "Zayıf", "Orta", "Güçlü", "Çok güçlü")[score]
        return Strength(score, label, bits, warnings)
    }

    private fun hasSequence(s: String): Boolean {
        for (i in 0..s.length - 3) {
            val a = s[i].code; val b = s[i + 1].code; val c = s[i + 2].code
            if ((b == a + 1 && c == b + 1) || (b == a - 1 && c == b - 1)) return true
        }
        return false
    }
}

package com.galaxyoffline.core.crypto

import java.net.URLDecoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** RFC 4648 Base32 (boşluk, tire ve dolgu yok sayılır, büyük/küçük harf duyarsız). */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(input: String): ByteArray {
        val clean = input.uppercase().filter { it != ' ' && it != '-' && it != '=' }
        require(clean.isNotEmpty()) { "Gizli anahtar boş" }
        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (ch in clean) {
            val v = ALPHABET.indexOf(ch)
            require(v >= 0) { "Geçersiz karakter: $ch" }
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                out.write((buffer shr (bits - 8)) and 0xFF)
                bits -= 8
            }
        }
        return out.toByteArray()
    }

    fun encode(data: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                sb.append(ALPHABET[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) sb.append(ALPHABET[(buffer shl (5 - bits)) and 31])
        return sb.toString()
    }

    fun isValid(input: String) = runCatching { decode(input).size >= 10 }.getOrDefault(false)
}

enum class OtpAlgorithm(val mac: String) { SHA1("HmacSHA1"), SHA256("HmacSHA256"), SHA512("HmacSHA512");
    companion object { fun of(s: String?) = entries.firstOrNull { it.name.equals(s, true) } ?: SHA1 } }

/** RFC 4226 (HOTP) ve RFC 6238 (TOTP). Kodlar tamamen cihazda, saat bilgisiyle üretilir. */
object Totp {
    fun hotp(secret: ByteArray, counter: Long, digits: Int = 6, algorithm: OtpAlgorithm = OtpAlgorithm.SHA1): String {
        val msg = ByteArray(8) { i -> (counter ushr (8 * (7 - i))).toByte() }
        val mac = Mac.getInstance(algorithm.mac).apply { init(SecretKeySpec(secret, algorithm.mac)) }
        val h = mac.doFinal(msg)
        val o = h.last().toInt() and 0x0F
        val bin = ((h[o].toInt() and 0x7F) shl 24) or ((h[o + 1].toInt() and 0xFF) shl 16) or
            ((h[o + 2].toInt() and 0xFF) shl 8) or (h[o + 3].toInt() and 0xFF)
        var mod = 1
        repeat(digits) { mod *= 10 }
        return (bin % mod).toString().padStart(digits, '0')
    }

    fun code(secret: ByteArray, unixSeconds: Long, period: Int = 30, digits: Int = 6, algorithm: OtpAlgorithm = OtpAlgorithm.SHA1) =
        hotp(secret, Math.floorDiv(unixSeconds, period.toLong()), digits, algorithm)

    /** Mevcut kodun kalan geçerlilik süresi (sn). */
    fun secondsRemaining(unixSeconds: Long, period: Int = 30) = period - (Math.floorMod(unixSeconds, period.toLong())).toInt()
}

data class OtpAccount(
    val issuer: String,
    val account: String,
    val secret: String,
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
)

/** otpauth://totp/Issuer:hesap?secret=...&issuer=...&algorithm=SHA1&digits=6&period=30 */
object OtpAuthUri {
    class ParseException(message: String) : Exception(message)

    fun parse(uri: String): OtpAccount {
        val s = uri.trim()
        if (!s.startsWith("otpauth://", ignoreCase = true)) throw ParseException("Bu bir doğrulama (otpauth) kodu değil")
        val rest = s.substring("otpauth://".length)
        val type = rest.substringBefore('/').lowercase()
        if (type == "hotp") throw ParseException("Sayaç tabanlı (HOTP) hesaplar desteklenmiyor; yalnızca TOTP")
        if (type != "totp") throw ParseException("Bilinmeyen tür: $type")
        val label = decode(rest.substringAfter('/').substringBefore('?'))
        val params = rest.substringAfter('?', "").split('&').filter { '=' in it }
            .associate { decode(it.substringBefore('=')).lowercase() to decode(it.substringAfter('=')) }
        val secret = params["secret"]?.replace(" ", "")?.uppercase() ?: throw ParseException("Gizli anahtar (secret) yok")
        if (!Base32.isValid(secret)) throw ParseException("Gizli anahtar geçersiz")
        val labelIssuer = if (':' in label) label.substringBefore(':').trim() else ""
        val account = if (':' in label) label.substringAfter(':').trim() else label.trim()
        val digits = params["digits"]?.toIntOrNull() ?: 6
        val period = params["period"]?.toIntOrNull() ?: 30
        if (digits !in 6..8) throw ParseException("Desteklenmeyen basamak sayısı: $digits")
        if (period !in 10..300) throw ParseException("Desteklenmeyen süre: $period")
        return OtpAccount(params["issuer"]?.trim()?.ifBlank { null } ?: labelIssuer, account, secret,
            OtpAlgorithm.of(params["algorithm"]), digits, period)
    }

    fun build(a: OtpAccount): String {
        fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20")
        val label = if (a.issuer.isNotBlank()) "${enc(a.issuer)}:${enc(a.account)}" else enc(a.account)
        return "otpauth://totp/$label?secret=${a.secret}&issuer=${enc(a.issuer)}&algorithm=${a.algorithm.name}&digits=${a.digits}&period=${a.period}"
    }

    private fun decode(v: String) = URLDecoder.decode(v.replace("+", "%2B"), "UTF-8")
}

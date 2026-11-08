package com.galaxyoffline.qrcode.data

/** QR içeriğinin türünü tanır (saf Kotlin, test edilebilir). */
sealed interface QrContent {
    val raw: String
    val title: String
    data class Url(override val raw: String, val url: String) : QrContent { override val title = "Bağlantı"; val secure get() = url.startsWith("https://", true) }
    data class Wifi(override val raw: String, val ssid: String, val password: String, val security: String, val hidden: Boolean) : QrContent { override val title = "Wi-Fi ağı" }
    data class Contact(override val raw: String, val name: String, val phones: List<String>, val emails: List<String>, val org: String) : QrContent { override val title = "Kişi kartı" }
    data class Phone(override val raw: String, val number: String) : QrContent { override val title = "Telefon numarası" }
    data class Sms(override val raw: String, val number: String, val body: String) : QrContent { override val title = "SMS" }
    data class Email(override val raw: String, val to: String, val subject: String, val body: String) : QrContent { override val title = "E-posta" }
    data class Geo(override val raw: String, val lat: Double, val lon: Double) : QrContent { override val title = "Konum" }
    data class Text(override val raw: String) : QrContent { override val title = "Metin" }
    data class Product(override val raw: String) : QrContent { override val title = "Ürün barkodu" }
}

object QrParser {
    fun parse(raw: String, format: String = "QR kod"): QrContent {
        val s = raw.trim()
        val lower = s.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") -> QrContent.Url(raw, s)
            lower.startsWith("www.") -> QrContent.Url(raw, "https://$s")
            lower.startsWith("wifi:") -> wifi(raw, s.substring(5))
            lower.startsWith("begin:vcard") -> vcard(raw, s)
            lower.startsWith("mecard:") -> mecard(raw, s.substring(7))
            lower.startsWith("tel:") -> QrContent.Phone(raw, s.substring(4))
            lower.startsWith("smsto:") || lower.startsWith("sms:") -> {
                val body = s.substringAfter(':'); QrContent.Sms(raw, body.substringBefore(':'), body.substringAfter(':', ""))
            }
            lower.startsWith("mailto:") -> {
                val addr = s.substring(7).substringBefore('?')
                val q = params(s.substringAfter('?', ""))
                QrContent.Email(raw, addr, q["subject"].orEmpty(), q["body"].orEmpty())
            }
            lower.startsWith("matmsg:") -> {
                val f = fields(s.substring(7)); QrContent.Email(raw, f["TO"].orEmpty(), f["SUB"].orEmpty(), f["BODY"].orEmpty())
            }
            lower.startsWith("geo:") -> {
                val p = s.substring(4).substringBefore('?').split(',')
                val lat = p.getOrNull(0)?.toDoubleOrNull(); val lon = p.getOrNull(1)?.toDoubleOrNull()
                if (lat != null && lon != null) QrContent.Geo(raw, lat, lon) else QrContent.Text(raw)
            }
            format != "QR kod" && s.all { it.isDigit() } -> QrContent.Product(raw)
            else -> QrContent.Text(raw)
        }
    }

    /** "T:WPA;S:ağ;P:şifre;H:false;;" — kaçış karakterli (\;) değerleri destekler. */
    private fun fields(body: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val sb = StringBuilder(); var key: String? = null; var i = 0
        while (i < body.length) {
            val c = body[i]
            when {
                c == '\\' && i + 1 < body.length -> { sb.append(body[i + 1]); i++ }
                key == null && c == ':' -> { key = sb.toString().uppercase(); sb.clear() }
                c == ';' -> { if (key != null) out.putIfAbsent(key, sb.toString()) else if (sb.isNotEmpty()) Unit; key = null; sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        if (key != null) out.putIfAbsent(key, sb.toString())
        return out
    }

    private fun wifi(raw: String, body: String): QrContent {
        val f = fields(body)
        return QrContent.Wifi(raw, f["S"].orEmpty(), f["P"].orEmpty(), f["T"]?.ifBlank { null } ?: "Açık", f["H"].equals("true", true))
    }

    private fun mecard(raw: String, body: String): QrContent {
        val parts = mutableListOf<Pair<String, String>>()
        body.split(';').forEach { p -> if (':' in p) parts += p.substringBefore(':').uppercase() to p.substringAfter(':') }
        val name = parts.firstOrNull { it.first == "N" }?.second?.split(',')?.reversed()?.joinToString(" ")?.trim().orEmpty()
        return QrContent.Contact(raw, name, parts.filter { it.first == "TEL" }.map { it.second }, parts.filter { it.first == "EMAIL" }.map { it.second },
            parts.firstOrNull { it.first == "ORG" }?.second.orEmpty())
    }

    private fun vcard(raw: String, s: String): QrContent {
        val lines = s.lines().map { it.trim() }
        fun value(l: String) = l.substringAfter(':')
        val fn = lines.firstOrNull { it.uppercase().startsWith("FN") }?.let(::value)
            ?: lines.firstOrNull { it.uppercase().startsWith("N:") || it.uppercase().startsWith("N;") }?.let { value(it).split(';').filter(String::isNotBlank).reversed().joinToString(" ") }
        return QrContent.Contact(raw, fn.orEmpty(), lines.filter { it.uppercase().startsWith("TEL") }.map(::value),
            lines.filter { it.uppercase().startsWith("EMAIL") }.map(::value), lines.firstOrNull { it.uppercase().startsWith("ORG") }?.let(::value).orEmpty())
    }

    private fun params(q: String) = q.split('&').filter { '=' in it }.associate {
        it.substringBefore('=').lowercase() to java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8")
    }
}

/** QR oluşturma yükleri. */
object QrPayload {
    private fun esc(v: String) = v.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")
    fun wifi(ssid: String, password: String, security: String, hidden: Boolean) =
        "WIFI:T:${if (security == "Açık") "nopass" else security};S:${esc(ssid)};" + (if (security != "Açık") "P:${esc(password)};" else "") + (if (hidden) "H:true;" else "") + ";"
    fun contact(name: String, phone: String, email: String, org: String) = buildString {
        appendLine("BEGIN:VCARD"); appendLine("VERSION:3.0"); appendLine("FN:$name")
        if (phone.isNotBlank()) appendLine("TEL:$phone"); if (email.isNotBlank()) appendLine("EMAIL:$email"); if (org.isNotBlank()) appendLine("ORG:$org")
        append("END:VCARD")
    }
    fun url(u: String) = if (u.contains("://")) u.trim() else "https://${u.trim()}"
}

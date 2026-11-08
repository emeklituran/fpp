package com.galaxyoffline.core.math

/**
 * Hesap makinesi tuşlarının ifade metnine etkisi (saf ve test edilebilir).
 * İç biçim: "." ondalık, "−" eksi, "×", "÷", "^"; fonksiyonlar "sin(" gibi parantezle eklenir.
 */
object ExpressionEditor {
    private const val MINUS = ExpressionEngine.MINUS
    private val binaryOps = setOf('+', MINUS, '×', '÷', '^')
    private val functionTokens = listOf("asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "sin(", "cos(", "tan(",
        "log2(", "log(", "ln(", "abs(", "exp(", "√(", "∛(", "Ans")
    const val MAX_LENGTH = 200

    private fun endsWithOperand(e: String): Boolean {
        if (e.isEmpty()) return false
        val c = e.last()
        return c.isDigit() || c == ')' || c == 'π' || c == 'e' || c == '!' || c == '%' || e.endsWith("Ans") || c == '.'
    }

    fun digit(e: String, d: Char): String = if (e.length >= MAX_LENGTH) e else e + d

    fun decimal(e: String): String {
        val run = e.takeLastWhile { it.isDigit() || it == '.' }
        return when {
            '.' in run -> e
            run.isEmpty() -> e + "0."
            else -> e + "."
        }
    }

    fun operator(e: String, op: Char): String {
        if (e.length >= MAX_LENGTH) return e
        if (e.isEmpty()) return if (op == MINUS) "$MINUS" else e
        val last = e.last()
        return when {
            last == '(' -> if (op == MINUS) e + op else e
            last in binaryOps -> when {
                // 5×−3 gibi tekli eksiye izin ver
                op == MINUS && last != '+' && last != MINUS -> e + op
                e.length >= 2 && e[e.length - 2] in binaryOps -> e.dropLast(2) + op
                e.length == 1 -> if (op == MINUS) "$MINUS" else ""
                else -> e.dropLast(1) + op
            }
            last == '.' -> e.dropLast(1) + op
            else -> e + op
        }
    }

    fun postfix(e: String, c: Char): String {
        if (!endsWithOperand(e)) return e
        if (c == '%' && e.last() == '%') return e
        return if (e.last() == '.') e.dropLast(1) + c else e + c
    }

    fun openCount(e: String) = e.count { it == '(' } - e.count { it == ')' }

    /** Tek "( )" tuşu: duruma göre açar veya kapatır. */
    fun smartParen(e: String): String =
        if (openCount(e) > 0 && endsWithOperand(e)) e + ")" else e + "("

    fun open(e: String) = if (e.length >= MAX_LENGTH) e else e + "("
    fun close(e: String) = if (openCount(e) > 0 && endsWithOperand(e)) e + ")" else e

    /** Fonksiyon/parça ekler. "10^(" gibi rakamla başlayan parça bir sayının ardına gelirse araya × konur. */
    fun function(e: String, name: String): String {
        if (e.length >= MAX_LENGTH) return e
        val needsTimes = name.first().isDigit() && e.isNotEmpty() && (e.last().isDigit() || e.last() == '.')
        return if (needsTimes) e + "×" + name else e + name
    }

    /** Bilimsel gösterim "E": yalnızca bir rakamdan sonra ve sayıda henüz E yoksa. */
    fun exponent(e: String): String {
        if (e.isEmpty() || !e.last().isDigit()) return e
        val run = e.takeLastWhile { it.isDigit() || it == '.' }
        val before = e.dropLast(run.length)
        return if (before.endsWith("E") || before.endsWith("E−") || before.endsWith("E+")) e else e + "E"
    }

    fun constant(e: String, c: String): String = if (e.length >= MAX_LENGTH) e else e + c

    fun backspace(e: String): String {
        if (e.isEmpty()) return e
        functionTokens.firstOrNull { e.endsWith(it) }?.let { return e.dropLast(it.length) }
        return e.dropLast(1)
    }

    /** ± : son sayıyı (−x) biçimine çevirir veya geri alır. */
    fun toggleSign(e: String): String {
        if (e.isEmpty()) return "$MINUS"
        val run = e.takeLastWhile { it.isDigit() || it == '.' || it == 'π' || it == 'e' }
        if (run.isEmpty()) return e
        val prefix = e.dropLast(run.length)
        return if (prefix.endsWith("($MINUS")) prefix.dropLast(2) + run else "$prefix($MINUS$run"
    }

    /** "=" sonrası: tamamlanmamış parantezleri kapatır. */
    fun closeAll(e: String): String = e + ")".repeat(openCount(e).coerceAtLeast(0))
}

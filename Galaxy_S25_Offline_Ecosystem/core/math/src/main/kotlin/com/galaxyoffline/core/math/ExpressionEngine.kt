package com.galaxyoffline.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

enum class AngleMode(val label: String) { DEG("DEG"), RAD("RAD") }

sealed interface CalcResult {
    data class Value(val value: Double) : CalcResult
    data class Error(val message: String) : CalcResult
}

class CalcException(message: String) : RuntimeException(message)

/**
 * Hesap makinesi ifade motoru (özyinelemeli iniş ayrıştırıcı).
 *
 * Desteklenenler: + − × ÷ ^, parantez (eksik kapanışlar otomatik tamamlanır), tekli eksi,
 * örtük çarpma (2π, 3(4), 2sin(30)), son ek % ve !, "akıllı yüzde" (200 + %10 = 220),
 * sabitler π ve e, Ans, bilimsel gösterim (1.5E3) ve fonksiyonlar:
 * sin cos tan asin acos atan sinh cosh tanh ln log log2 √ ∛ abs exp.
 */
object ExpressionEngine {
    const val MINUS = '−'
    private val OPERATORS = setOf('+', '-', MINUS, '×', '*', '÷', '/', '^')
    val FUNCTIONS = listOf("asin", "acos", "atan", "sinh", "cosh", "tanh", "sin", "cos", "tan", "log2", "log", "ln", "abs", "exp")

    fun evaluate(expression: String, angle: AngleMode = AngleMode.RAD, ans: Double = 0.0): CalcResult {
        if (expression.isBlank()) return CalcResult.Error("İfade boş")
        return try {
            val value = Parser(tokenize(expression), angle, ans).parseAll()
            when {
                value.isNaN() -> CalcResult.Error("Tanımsız")
                value.isInfinite() -> CalcResult.Error("Sonuç çok büyük")
                else -> CalcResult.Value(if (value == -0.0) 0.0 else value)
            }
        } catch (e: CalcException) {
            CalcResult.Error(e.message ?: "Hata")
        } catch (e: ArithmeticException) {
            CalcResult.Error("Hesaplanamadı")
        } catch (e: StackOverflowError) {
            CalcResult.Error("İfade çok karmaşık")
        }
    }

    /** Canlı önizleme: sondaki operatörler yok sayılır, hata durumunda null döner. */
    fun preview(expression: String, angle: AngleMode = AngleMode.RAD, ans: Double = 0.0): Double? {
        var e = expression.trimEnd()
        while (e.isNotEmpty() && (e.last() in OPERATORS || e.last() == '(')) e = e.dropLast(1)
        if (e.isEmpty()) return null
        if (e.all { it.isDigit() || it == '.' }) return null // tek sayıda önizleme gereksiz
        return (evaluate(e, angle, ans) as? CalcResult.Value)?.value
    }

    // ---------------- Sözcük çözümleyici ----------------

    internal sealed interface Token {
        data class Num(val v: Double) : Token
        data class Op(val c: Char) : Token
        data class Func(val name: String) : Token
        data class Post(val c: Char) : Token
        data object LParen : Token
        data object RParen : Token
    }

    internal fun tokenize(src: String): List<Token> {
        val out = ArrayList<Token>()
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < src.length && (src[i].isDigit() || src[i] == '.')) i++
                    if (i < src.length && src[i] == 'E') {
                        var j = i + 1
                        if (j < src.length && (src[j] == '+' || src[j] == '-' || src[j] == MINUS)) j++
                        if (j < src.length && src[j].isDigit()) {
                            while (j < src.length && src[j].isDigit()) j++
                            i = j
                        }
                    }
                    val text = src.substring(start, i).replace(MINUS, '-')
                    if (text.count { it == '.' } > 1 || text == ".") throw CalcException("Sözdizimi hatası")
                    out += Token.Num(text.toDoubleOrNull() ?: throw CalcException("Sözdizimi hatası"))
                }
                c == 'π' -> { out += Token.Num(PI); i++ }
                c == '√' -> { out += Token.Func("sqrt"); i++ }
                c == '∛' -> { out += Token.Func("cbrt"); i++ }
                c in OPERATORS -> {
                    out += Token.Op(
                        when (c) { '-' -> MINUS; '*' -> '×'; '/' -> '÷'; else -> c },
                    ); i++
                }
                c == '(' -> { out += Token.LParen; i++ }
                c == ')' -> { out += Token.RParen; i++ }
                c == '%' || c == '!' -> { out += Token.Post(c); i++ }
                src.startsWith("Ans", i) -> { out += Token.Func("ans"); i += 3 }
                c.isLetter() -> {
                    val name = FUNCTIONS.firstOrNull { src.startsWith(it, i) }
                    if (name != null) {
                        out += Token.Func(name); i += name.length
                    } else if (c == 'e') {
                        out += Token.Num(Math.E); i++
                    } else {
                        throw CalcException("Bilinmeyen simge: $c")
                    }
                }
                else -> throw CalcException("Bilinmeyen simge: $c")
            }
        }
        return out
    }

    // ---------------- Ayrıştırıcı ----------------

    private data class V(val value: Double, val percent: Boolean = false)

    private class Parser(val tokens: List<Token>, val angle: AngleMode, val ans: Double) {
        var pos = 0
        fun peek(): Token? = tokens.getOrNull(pos)
        fun next(): Token? = tokens.getOrNull(pos++)

        fun parseAll(): Double {
            if (tokens.isEmpty()) throw CalcException("İfade boş")
            val v = parseExpr().value
            if (pos < tokens.size) throw CalcException("Sözdizimi hatası")
            return v
        }

        fun parseExpr(): V {
            var acc = parseTerm()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.c == '+' || t.c == MINUS)) {
                    next()
                    val r = parseTerm()
                    val delta = if (r.percent) acc.value * r.value else r.value
                    acc = V(if (t.c == '+') acc.value + delta else acc.value - delta)
                } else return acc
            }
        }

        fun startsOperand(t: Token?) = t is Token.Num || t is Token.Func || t == Token.LParen

        fun parseTerm(): V {
            var left = parseUnary()
            while (true) {
                val t = peek()
                when {
                    t is Token.Op && (t.c == '×' || t.c == '÷') -> {
                        next()
                        val r = parseUnary()
                        left = if (t.c == '×') V(left.value * r.value) else {
                            if (r.value == 0.0) throw CalcException("Sıfıra bölünemez")
                            V(left.value / r.value)
                        }
                    }
                    startsOperand(t) -> left = V(left.value * parseUnary().value)
                    else -> return left
                }
            }
        }

        fun parseUnary(): V {
            val t = peek()
            if (t is Token.Op && t.c == MINUS) { next(); val v = parseUnary(); return V(-v.value, v.percent) }
            if (t is Token.Op && t.c == '+') { next(); return parseUnary() }
            return parsePower()
        }

        fun parsePower(): V {
            val base = parsePostfix()
            val t = peek()
            if (t is Token.Op && t.c == '^') {
                next()
                val exp = parseUnary().value
                return V(base.value.pow(exp))
            }
            return base
        }

        fun parsePostfix(): V {
            var v = parsePrimary()
            while (true) {
                val t = peek()
                if (t is Token.Post) {
                    next()
                    v = if (t.c == '%') V(v.value / 100.0, percent = true) else V(factorial(v.value))
                } else return v
            }
        }

        fun parsePrimary(): V = when (val t = next()) {
            is Token.Num -> V(t.v)
            Token.LParen -> {
                val inner = parseExpr()
                if (peek() == Token.RParen) next() // eksik kapanış parantezi tolere edilir
                V(inner.value)
            }
            is Token.Func -> {
                if (t.name == "ans") V(ans) else {
                    val arg = if (peek() == Token.LParen) {
                        next()
                        val a = parseExpr().value
                        if (peek() == Token.RParen) next()
                        a
                    } else parsePower().value
                    V(apply(t.name, arg))
                }
            }
            else -> throw CalcException("Sözdizimi hatası")
        }

        fun toRad(x: Double) = if (angle == AngleMode.DEG) x * PI / 180.0 else x
        fun fromRad(x: Double) = if (angle == AngleMode.DEG) x * 180.0 / PI else x
        fun clean(x: Double) = if (abs(x) < 1e-12) 0.0 else x

        fun apply(name: String, x: Double): Double = when (name) {
            "sin" -> clean(kotlin.math.sin(toRad(x)))
            "cos" -> clean(kotlin.math.cos(toRad(x)))
            "tan" -> {
                if (angle == AngleMode.DEG && abs(((x % 180.0) + 180.0) % 180.0 - 90.0) < 1e-9) throw CalcException("Tanımsız")
                val c = kotlin.math.cos(toRad(x))
                if (abs(c) < 1e-15) throw CalcException("Tanımsız")
                clean(kotlin.math.tan(toRad(x)))
            }
            "asin" -> { if (abs(x) > 1) throw CalcException("Tanımsız"); fromRad(kotlin.math.asin(x)) }
            "acos" -> { if (abs(x) > 1) throw CalcException("Tanımsız"); fromRad(kotlin.math.acos(x)) }
            "atan" -> fromRad(kotlin.math.atan(x))
            "sinh" -> kotlin.math.sinh(x)
            "cosh" -> kotlin.math.cosh(x)
            "tanh" -> kotlin.math.tanh(x)
            "ln" -> { if (x <= 0) throw CalcException("Tanımsız"); kotlin.math.ln(x) }
            "log" -> { if (x <= 0) throw CalcException("Tanımsız"); log10(x) }
            "log2" -> { if (x <= 0) throw CalcException("Tanımsız"); kotlin.math.log2(x) }
            "sqrt" -> { if (x < 0) throw CalcException("Tanımsız"); kotlin.math.sqrt(x) }
            "cbrt" -> Math.cbrt(x)
            "abs" -> abs(x)
            "exp" -> kotlin.math.exp(x)
            else -> throw CalcException("Bilinmeyen fonksiyon")
        }

        fun factorial(x: Double): Double {
            if (x < 0 || x != floor(x)) throw CalcException("Faktöriyel yalnızca doğal sayılar için")
            if (x > 170) throw CalcException("Sonuç çok büyük")
            var r = 1.0
            var i = 2
            while (i <= x.toInt()) { r *= i; i++ }
            return r
        }
    }
}

/** Sonuç ve ifade biçimlendirme (Türkçe: ondalık virgül, binlik nokta). */
object CalcFormat {
    private const val SIGNIFICANT = 12

    fun result(value: Double, grouping: Boolean = true): String {
        if (value.isNaN()) return "Tanımsız"
        if (value.isInfinite()) return "∞"
        if (value == 0.0) return "0"
        val a = abs(value)
        if (a >= 1e15 || a < 1e-9) {
            val exp = floor(log10(a)).toInt()
            val mantissa = BigDecimal(value / 10.0.pow(exp)).round(MathContext(10, RoundingMode.HALF_UP))
                .stripTrailingZeros().toPlainString().replace('.', ',').replace('-', ExpressionEngine.MINUS)
            return "${mantissa}E$exp"
        }
        val plain = BigDecimal(value).round(MathContext(SIGNIFICANT, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString()
        return displayNumber(plain, grouping)
    }

    /** Sonucu ifadeye yeniden eklemek için iç biçim ("." ondalık, "−" eksi). */
    fun literal(value: Double): String {
        val a = abs(value)
        val body = if (a != 0.0 && (a >= 1e15 || a < 1e-9)) {
            val exp = floor(log10(a)).toInt()
            BigDecimal(a / 10.0.pow(exp)).round(MathContext(10)).stripTrailingZeros().toPlainString() + "E" + exp
        } else {
            BigDecimal(a).round(MathContext(SIGNIFICANT)).stripTrailingZeros().toPlainString()
        }
        return if (value < 0) "${ExpressionEngine.MINUS}$body" else body
    }

    /** İfadeyi ekranda göstermek için: sayılara binlik ayırıcı ve ondalık virgül eklenir. */
    fun expression(expr: String, grouping: Boolean = true): String {
        val sb = StringBuilder()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                val start = i
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                // Bilimsel gösterimdeki üs kısmı gruplanmaz.
                val afterE = start > 0 && expr[start - 1] == 'E' ||
                    start > 1 && expr[start - 1] in "+-−" && expr[start - 2] == 'E'
                sb.append(if (afterE) expr.substring(start, i) else displayNumber(expr.substring(start, i), grouping))
            } else {
                sb.append(if (c == '-') ExpressionEngine.MINUS else c)
                i++
            }
        }
        return sb.toString()
    }

    private fun displayNumber(raw: String, grouping: Boolean): String {
        val negative = raw.startsWith("-")
        val s = raw.removePrefix("-")
        val dot = s.indexOf('.')
        val intPart = if (dot >= 0) s.substring(0, dot) else s
        val frac = if (dot >= 0) s.substring(dot + 1) else null
        val grouped = if (grouping && intPart.length > 3) {
            intPart.reversed().chunked(3).joinToString(".").reversed()
        } else intPart
        val body = if (frac != null) "$grouped,$frac" else grouped
        return if (negative) "${ExpressionEngine.MINUS}$body" else body
    }
}

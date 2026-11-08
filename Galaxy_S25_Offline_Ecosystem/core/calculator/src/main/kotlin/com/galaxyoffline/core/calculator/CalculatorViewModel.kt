package com.galaxyoffline.core.calculator

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.core.math.AngleMode
import com.galaxyoffline.core.math.CalcFormat
import com.galaxyoffline.core.math.CalcResult
import com.galaxyoffline.core.math.ExpressionEditor
import com.galaxyoffline.core.math.ExpressionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tuş olayları. Her uygulama kendi klavyesinde bunların bir alt kümesini kullanır. */
sealed interface Key {
    data class Digit(val d: Char) : Key
    data object Decimal : Key
    data class Op(val op: Char) : Key
    data class Postfix(val c: Char) : Key
    data object SmartParen : Key
    data object Open : Key
    data object Close : Key
    /** Fonksiyon veya ifadeye doğrudan eklenen parça: "sin(", "√(", "^2", "10^(" ... */
    data class Insert(val token: String, val operandFirst: Boolean = false) : Key
    data object Backspace : Key
    data object Clear : Key
    data object Equals : Key
    data object Sign : Key
    data object MemoryClear : Key
    data object MemoryRecall : Key
    data object MemoryAdd : Key
    data object MemorySubtract : Key
    data object ToggleAngle : Key
    data object ToggleInverse : Key
    data object Exponent : Key
}

data class CalcUiState(
    val expression: String = "",
    val preview: String? = null,
    val error: String? = null,
    val justEvaluated: Boolean = false,
    val memory: Double? = null,
    val angle: AngleMode = AngleMode.DEG,
    val inverse: Boolean = false,
    val grouping: Boolean = true,
    val haptics: Boolean = true,
)

class CalculatorViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = GalaxyPreferences.get(app)
    private val dao = HistoryDatabase.get(app).history()
    val messages = UiMessages()

    private val _state = MutableStateFlow(CalcUiState())
    val state: StateFlow<CalcUiState> = _state.asStateFlow()

    val history: StateFlow<List<HistoryEntry>> = dao.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var lastAnswer = 0.0

    init {
        viewModelScope.launch {
            prefs.data.collect { p ->
                _state.update {
                    it.copy(
                        memory = p[KEY_MEMORY]?.toDoubleOrNull(),
                        angle = if (p[KEY_ANGLE] == AngleMode.RAD.name) AngleMode.RAD else AngleMode.DEG,
                        grouping = p[KEY_GROUPING] ?: true,
                        haptics = p[KEY_HAPTICS] ?: true,
                    )
                }
            }
        }
    }

    fun onKey(key: Key) {
        val s = _state.value
        // "=" sonrası: rakam yeni ifade başlatır, operatör sonuçtan devam eder.
        val base = if (s.justEvaluated && (key is Key.Digit || key == Key.Decimal || key is Key.Insert && !key.operandFirst ||
                key == Key.Open || key == Key.SmartParen)) "" else s.expression
        val e = base
        val next: String = when (key) {
            is Key.Digit -> ExpressionEditor.digit(e, key.d)
            Key.Decimal -> ExpressionEditor.decimal(e)
            is Key.Op -> ExpressionEditor.operator(e, key.op)
            is Key.Postfix -> ExpressionEditor.postfix(e, key.c)
            Key.SmartParen -> ExpressionEditor.smartParen(e)
            Key.Open -> ExpressionEditor.open(e)
            Key.Close -> ExpressionEditor.close(e)
            is Key.Insert -> if (key.operandFirst && e.isEmpty()) e else ExpressionEditor.function(e, key.token)
            Key.Backspace -> if (s.justEvaluated) "" else ExpressionEditor.backspace(e)
            Key.Clear -> ""
            Key.Sign -> ExpressionEditor.toggleSign(e)
            Key.Exponent -> ExpressionEditor.exponent(e)
            Key.Equals -> { evaluate(); return }
            Key.MemoryClear -> { saveMemory(null); return }
            Key.MemoryRecall -> {
                val m = s.memory ?: return
                val lit = CalcFormat.literal(m)
                if (s.justEvaluated || e.isEmpty()) lit else e + lit
            }
            Key.MemoryAdd, Key.MemorySubtract -> {
                val v = currentValue() ?: run { messages.post("Belleğe eklenecek geçerli bir sonuç yok"); return }
                val m = (s.memory ?: 0.0) + if (key == Key.MemoryAdd) v else -v
                saveMemory(m)
                messages.post("Bellek: ${CalcFormat.result(m, s.grouping)}")
                return
            }
            Key.ToggleAngle -> {
                val a = if (s.angle == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
                launchSafe { prefs.edit { it[KEY_ANGLE] = a.name } }
                _state.update { it.copy(angle = a) }
                refreshPreview()
                return
            }
            Key.ToggleInverse -> { _state.update { it.copy(inverse = !it.inverse) }; return }
        }
        _state.update { it.copy(expression = next, error = null, justEvaluated = false) }
        refreshPreview()
    }

    private fun currentValue(): Double? {
        val s = _state.value
        if (s.expression.isEmpty()) return null
        return (ExpressionEngine.evaluate(ExpressionEditor.closeAll(s.expression), s.angle, lastAnswer) as? CalcResult.Value)?.value
    }

    private fun refreshPreview() {
        val s = _state.value
        val v = ExpressionEngine.preview(ExpressionEditor.closeAll(s.expression), s.angle, lastAnswer)
        _state.update { it.copy(preview = v?.let { x -> CalcFormat.result(x, s.grouping) }) }
    }

    private fun evaluate() {
        val s = _state.value
        if (s.expression.isEmpty()) return
        val expr = ExpressionEditor.closeAll(s.expression)
        when (val r = ExpressionEngine.evaluate(expr, s.angle, lastAnswer)) {
            is CalcResult.Error -> _state.update { it.copy(error = r.message, preview = null) }
            is CalcResult.Value -> {
                lastAnswer = r.value
                val shown = CalcFormat.expression(expr, s.grouping)
                val result = CalcFormat.result(r.value, s.grouping)
                if (!s.justEvaluated) {
                    launchSafe(messages, "Geçmiş kaydedilemedi") {
                        dao.insert(HistoryEntry(rawExpression = expr, expression = shown, result = result, value = r.value,
                            createdAt = System.currentTimeMillis()))
                        dao.trim(MAX_HISTORY)
                    }
                }
                _state.update {
                    it.copy(expression = CalcFormat.literal(r.value), preview = null, error = null, justEvaluated = true)
                }
            }
        }
    }

    fun useExpression(entry: HistoryEntry) {
        _state.update { it.copy(expression = entry.rawExpression, error = null, justEvaluated = false) }
        refreshPreview()
    }

    fun useResult(entry: HistoryEntry) {
        val s = _state.value
        val lit = CalcFormat.literal(entry.value)
        val e = if (s.justEvaluated || s.expression.isEmpty()) lit else s.expression + lit
        _state.update { it.copy(expression = e, error = null, justEvaluated = false) }
        refreshPreview()
    }

    fun deleteHistory(entry: HistoryEntry) = launchSafe(messages) { dao.delete(entry.id) }
    fun clearHistory() = launchSafe(messages) { dao.clear(); messages.post("Geçmiş temizlendi") }

    fun setGrouping(v: Boolean) = launchSafe(messages) { prefs.edit { it[KEY_GROUPING] = v } }
    fun setHaptics(v: Boolean) = launchSafe(messages) { prefs.edit { it[KEY_HAPTICS] = v } }

    private fun saveMemory(v: Double?) = launchSafe(messages) {
        prefs.edit { if (v == null) it.remove(KEY_MEMORY) else it[KEY_MEMORY] = v.toString() }
    }

    /** Test ve ekran döndürme sonrası için: kalıcı ayarlar okunana kadar bekler. */
    suspend fun awaitSettings() { prefs.data.first() }

    companion object {
        const val MAX_HISTORY = 500
        private val KEY_MEMORY = stringPreferencesKey("bellek")
        private val KEY_ANGLE = stringPreferencesKey("aci_modu")
        private val KEY_GROUPING = booleanPreferencesKey("binlik_ayirici")
        private val KEY_HAPTICS = booleanPreferencesKey("titresim")
    }
}

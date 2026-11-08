package com.fpp.finansasistani.data

import com.fpp.finans.core.Debt
import com.fpp.finans.core.DebtType
import com.fpp.finans.core.FinanceData
import com.fpp.finans.core.FixedExpense
import com.fpp.finans.core.Income
import com.fpp.finans.core.IncomeType
import com.fpp.finans.core.SavingGoal
import com.fpp.finans.core.SavingStrategy
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

class BackupFormatException(message: String) : Exception(message)

/** Yerel JSON yedek biçimi. Android'in yerleşik org.json kütüphanesi kullanılır (ek bağımlılık yok). */
object BackupSerializer {
    private const val FORMAT = "finans-asistani-yedek"
    private const val VERSION = 1
    private const val MAX_TEXT = 200
    private const val MAX_AMOUNT = 1e13

    fun toJson(data: FinanceData): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", Instant.now().toString())
            .put("incomes", JSONArray(data.incomes.map { it.toJson() }))
            .put("debts", JSONArray(data.debts.map { it.toJson() }))
            .put("expenses", JSONArray(data.expenses.map { it.toJson() }))
            .put("savings", JSONArray(data.savings.map { it.toJson() }))
        return root.toString(2)
    }

    fun fromJson(text: String): FinanceData {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupFormatException("Dosya geçerli bir JSON yedeği değil.")
        }
        if (root.optString("format") != FORMAT) {
            throw BackupFormatException("Bu dosya bir Finans Asistanı yedeği değil.")
        }
        val version = root.optInt("version", -1)
        if (version < 1 || version > VERSION) {
            throw BackupFormatException("Yedek sürümü ($version) bu uygulama sürümüyle uyumlu değil.")
        }
        return try {
            FinanceData(
                incomes = root.objects("incomes").map { it.toIncome() },
                debts = root.objects("debts").map { it.toDebt() },
                expenses = root.objects("expenses").map { it.toExpense() },
                savings = root.objects("savings").map { it.toSaving() },
            )
        } catch (e: JSONException) {
            throw BackupFormatException("Yedek dosyası bozuk veya eksik: ${e.message}")
        }
    }

    private fun JSONObject.objects(key: String): List<JSONObject> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    private fun JSONObject.text(key: String): String =
        optString(key, "").trim().take(MAX_TEXT).ifBlank { "Adsız" }

    private fun JSONObject.amount(key: String): Double {
        val v = optDouble(key, 0.0)
        return if (v.isFinite()) v.coerceIn(0.0, MAX_AMOUNT) else 0.0
    }

    private fun Income.toJson() = JSONObject()
        .put("id", id).put("name", name).put("amount", amount).put("type", type.name)
        .put("raisesPerYear", raisesPerYear).put("raisePercent", raisePercent).put("createdAt", createdAt)

    private fun JSONObject.toIncome() = Income(
        id = optLong("id", 0),
        name = text("name"),
        amount = amount("amount"),
        type = IncomeType.fromKey(optString("type")),
        raisesPerYear = optInt("raisesPerYear", 0).coerceIn(0, 12),
        raisePercent = amount("raisePercent").coerceAtMost(1000.0),
        createdAt = optLong("createdAt", 0),
    )

    private fun Debt.toJson() = JSONObject()
        .put("id", id).put("name", name).put("type", type.name).put("balance", balance)
        .put("annualInterestRate", annualInterestRate).put("minPayment", minPayment)
        .put("cardLimit", cardLimit).put("remainingInstallments", remainingInstallments)
        .put("firstPaymentDate", firstPaymentDate?.toString() ?: JSONObject.NULL)
        .put("createdAt", createdAt)

    private fun JSONObject.toDebt() = Debt(
        id = optLong("id", 0),
        name = text("name"),
        type = DebtType.fromKey(optString("type")),
        balance = amount("balance"),
        annualInterestRate = amount("annualInterestRate").coerceAtMost(10_000.0),
        minPayment = amount("minPayment"),
        cardLimit = amount("cardLimit"),
        remainingInstallments = optInt("remainingInstallments", 0).coerceIn(0, 1200),
        firstPaymentDate = if (isNull("firstPaymentDate")) null
        else runCatching { LocalDate.parse(optString("firstPaymentDate")) }.getOrNull(),
        createdAt = optLong("createdAt", 0),
    )

    private fun FixedExpense.toJson() = JSONObject()
        .put("id", id).put("name", name).put("amount", amount).put("createdAt", createdAt)

    private fun JSONObject.toExpense() = FixedExpense(
        id = optLong("id", 0),
        name = text("name"),
        amount = amount("amount"),
        createdAt = optLong("createdAt", 0),
    )

    private fun SavingGoal.toJson() = JSONObject()
        .put("id", id).put("name", name).put("strategy", strategy.name)
        .put("monthlyAmount", monthlyAmount).put("percent", percent).put("createdAt", createdAt)

    private fun JSONObject.toSaving() = SavingGoal(
        id = optLong("id", 0),
        name = text("name"),
        strategy = SavingStrategy.fromKey(optString("strategy")),
        monthlyAmount = amount("monthlyAmount"),
        percent = amount("percent").coerceAtMost(100.0),
        createdAt = optLong("createdAt", 0),
    )
}

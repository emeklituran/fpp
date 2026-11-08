package com.fpp.finansasistani.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpp.finans.core.CashFlow
import com.fpp.finans.core.Debt
import com.fpp.finans.core.FinanceCalculator
import com.fpp.finans.core.FinanceData
import com.fpp.finans.core.FixedExpense
import com.fpp.finans.core.Income
import com.fpp.finans.core.SavingGoal
import com.fpp.finansasistani.data.FinanceRepository
import com.fpp.finansasistani.util.MessageBus
import com.fpp.finansasistani.util.launchSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FinanceUiState(
    val loading: Boolean = true,
    val data: FinanceData = FinanceData(),
    val cashFlow: CashFlow = FinanceCalculator.cashFlow(FinanceData()),
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val bus: MessageBus,
) : ViewModel() {

    val state: StateFlow<FinanceUiState> = repository.data
        .map { FinanceUiState(loading = false, data = it, cashFlow = FinanceCalculator.cashFlow(it)) }
        .flowOn(Dispatchers.Default)
        .catch { e ->
            Log.e(TAG, "Veriler yüklenemedi", e)
            bus.post("Veriler yüklenemedi.")
            emit(FinanceUiState(loading = false))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    fun save(income: Income, onDone: () -> Unit) = launchSafe(bus, "Gelir kaydedilemedi.") {
        repository.save(income); bus.post("Gelir kaydedildi."); onDone()
    }

    fun save(debt: Debt, onDone: () -> Unit) = launchSafe(bus, "Borç kaydedilemedi.") {
        repository.save(debt); bus.post("Borç kaydedildi."); onDone()
    }

    fun save(expense: FixedExpense, onDone: () -> Unit) = launchSafe(bus, "Gider kaydedilemedi.") {
        repository.save(expense); bus.post("Sabit gider kaydedildi."); onDone()
    }

    fun save(saving: SavingGoal, onDone: () -> Unit) = launchSafe(bus, "Birikim hedefi kaydedilemedi.") {
        repository.save(saving); bus.post("Birikim hedefi kaydedildi."); onDone()
    }

    /** Silinen kayıt, "Geri al" ile aynı kimlikle yeniden eklenebilir. */
    fun delete(item: Any) = launchSafe(bus, "Kayıt silinemedi.") {
        when (item) {
            is Income -> repository.delete(item)
            is Debt -> repository.delete(item)
            is FixedExpense -> repository.delete(item)
            is SavingGoal -> repository.delete(item)
        }
    }

    fun restore(item: Any) = launchSafe(bus, "Kayıt geri alınamadı.") {
        when (item) {
            is Income -> repository.save(item)
            is Debt -> repository.save(item)
            is FixedExpense -> repository.save(item)
            is SavingGoal -> repository.save(item)
        }
    }

    suspend fun loadIncome(id: Long): Income? = load { repository.getIncome(id) }
    suspend fun loadDebt(id: Long): Debt? = load { repository.getDebt(id) }
    suspend fun loadExpense(id: Long): FixedExpense? = load { repository.getExpense(id) }
    suspend fun loadSaving(id: Long): SavingGoal? = load { repository.getSaving(id) }

    private suspend fun <T> load(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Kayıt okunamadı", e)
        bus.post("Kayıt okunamadı.")
        null
    }

    private companion object {
        const val TAG = "FinanceViewModel"
    }
}

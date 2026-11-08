package com.fpp.finansasistani.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpp.finans.core.DebtSimulator
import com.fpp.finans.core.FinanceData
import com.fpp.finans.core.SimulationOutcome
import com.fpp.finans.core.SimulationResult
import com.fpp.finans.core.Strategy
import com.fpp.finansasistani.data.FinanceRepository
import com.fpp.finansasistani.data.SettingsRepository
import com.fpp.finansasistani.util.MessageBus
import com.fpp.finansasistani.util.launchSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

sealed interface SimulationUiState {
    data object Idle : SimulationUiState
    data object Running : SimulationUiState
    data class Done(
        val primary: SimulationResult,
        /** Karşılaştırma için diğer strateji (başarısızsa null). */
        val alternative: SimulationResult?,
        val input: FinanceData,
    ) : SimulationUiState
    data class Error(val message: String) : SimulationUiState
}

class SimulationViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository,
    private val bus: MessageBus,
) : ViewModel() {

    val strategy: StateFlow<Strategy> = settings.settings
        .map { it.strategy }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Strategy.AVALANCHE)

    private val _state = MutableStateFlow<SimulationUiState>(SimulationUiState.Idle)
    val state: StateFlow<SimulationUiState> = _state.asStateFlow()

    /** Simülasyondan sonra veriler değiştiyse sonuç "güncel değil" olarak işaretlenir. */
    val isStale: StateFlow<Boolean> = combine(_state, repository.data) { s, data ->
        s is SimulationUiState.Done && s.input != data
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var job: Job? = null

    fun selectStrategy(value: Strategy) {
        if (value == strategy.value) return
        launchSafe(bus) { settings.setStrategy(value) }
        // Sonuç zaten varsa iki strateji önceden hesaplandığı için anında yer değiştirilir.
        val current = _state.value
        if (current is SimulationUiState.Done && current.primary.strategy != value) {
            val alt = current.alternative
            _state.value = if (alt != null) {
                current.copy(primary = alt, alternative = current.primary)
            } else {
                SimulationUiState.Idle
            }
        }
    }

    fun run() {
        job?.cancel()
        _state.value = SimulationUiState.Running
        job = viewModelScope.launch {
            _state.value = try {
                val data = repository.snapshot()
                val selected = strategy.value
                val start = YearMonth.now()
                val (primary, alternative) = withContext(Dispatchers.Default) {
                    DebtSimulator.simulate(data, selected, start) to DebtSimulator.simulate(data, selected.other, start)
                }
                when (primary) {
                    is SimulationOutcome.Failure -> SimulationUiState.Error(primary.reason)
                    is SimulationOutcome.Success -> SimulationUiState.Done(
                        primary = primary.result,
                        alternative = (alternative as? SimulationOutcome.Success)?.result,
                        input = data,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SimulationViewModel", "Simülasyon hatası", e)
                SimulationUiState.Error("Simülasyon çalıştırılamadı. Lütfen verilerinizi kontrol edin.")
            }
        }
    }
}

package com.fpp.finansasistani.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fpp.finansasistani.AppContainer

class AppViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(FinanceViewModel::class.java) ->
            FinanceViewModel(c.financeRepository, c.messageBus)
        modelClass.isAssignableFrom(SimulationViewModel::class.java) ->
            SimulationViewModel(c.financeRepository, c.settingsRepository, c.messageBus)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(c.settingsRepository, c.backupRepository, c.financeRepository, c.crashLogger, c.messageBus)
        else -> throw IllegalArgumentException("Bilinmeyen ViewModel: ${modelClass.name}")
    } as T
}

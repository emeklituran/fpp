package com.fpp.finansasistani

import android.content.Context
import com.fpp.finansasistani.data.BackupRepository
import com.fpp.finansasistani.data.FinanceRepository
import com.fpp.finansasistani.data.SettingsRepository
import com.fpp.finansasistani.data.local.FinanceDatabase
import com.fpp.finansasistani.util.CrashLogger
import com.fpp.finansasistani.util.MessageBus

/** Basit, elle bağımlılık enjeksiyonu. Nesneler ilk kullanımda oluşturulur (hızlı açılış). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val crashLogger = CrashLogger(appContext)
    val messageBus = MessageBus()

    private val database by lazy { FinanceDatabase.build(appContext) }
    val financeRepository by lazy { FinanceRepository(database) }
    val settingsRepository by lazy { SettingsRepository(appContext) }
    val backupRepository by lazy { BackupRepository(appContext, financeRepository) }
}

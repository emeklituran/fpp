package com.galaxyoffline.notes

import android.app.Application
import com.galaxyoffline.core.designsystem.util.CrashLogger
import com.galaxyoffline.notes.data.NotesDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application() {
    /** Ekran kapansa bile tamamlanması gereken kayıtlar (ör. editörden çıkarken son kaydetme) için. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { NotesDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}

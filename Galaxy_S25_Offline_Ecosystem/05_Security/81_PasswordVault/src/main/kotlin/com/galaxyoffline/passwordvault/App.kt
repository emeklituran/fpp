package com.galaxyoffline.passwordvault

import android.app.Application
import com.galaxyoffline.core.designsystem.util.CrashLogger
import com.galaxyoffline.core.security.AutoLock
import com.galaxyoffline.passwordvault.data.VaultSession

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        AutoLock(timeoutMs = { VaultSession.autoLockMs }, onLock = { VaultSession.lock() }).install()
    }
}

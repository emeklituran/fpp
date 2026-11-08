package com.galaxyoffline.authenticator

import android.app.Application
import com.galaxyoffline.authenticator.data.LockState
import com.galaxyoffline.core.designsystem.util.CrashLogger
import com.galaxyoffline.core.security.AutoLock

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        AutoLock(timeoutMs = { 30_000 }, onLock = { LockState.lockIfEnabled() }).install()
    }
}

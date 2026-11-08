package com.fpp.finansasistani

import android.app.Application

class FinansApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.crashLogger.install()
    }
}

package com.galaxyoffline.chess

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.galaxyoffline.chess.data.EngineHolder
import com.galaxyoffline.core.designsystem.util.CrashLogger

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        // Uygulama arka plana geçince motor durdurulur (pil); bir süre sonra süreç de kapatılır (bellek).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = EngineHolder.get(this@App).onBackground()
            override fun onStart(owner: LifecycleOwner) = EngineHolder.get(this@App).onForeground()
        })
    }
}

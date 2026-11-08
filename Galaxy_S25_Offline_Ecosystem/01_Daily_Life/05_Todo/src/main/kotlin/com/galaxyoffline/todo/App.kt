package com.galaxyoffline.todo

import android.app.Application
import com.galaxyoffline.core.designsystem.util.CrashLogger
import com.galaxyoffline.todo.data.TodoDatabase

class App : Application() {
    val database by lazy { TodoDatabase.get(this) }
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}

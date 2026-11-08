package com.galaxyoffline.diary

import android.app.Application
import com.galaxyoffline.core.designsystem.util.CrashLogger
import com.galaxyoffline.diary.data.DiaryDatabase

class App : Application() {
    val database by lazy { DiaryDatabase.get(this) }
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}

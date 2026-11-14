package com.aliazouzi.arabicide

import android.app.Application
import com.aliazouzi.arabicide.diagnostics.CrashLogger

class ArabicIDEApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        CrashLogger.log(this, "app", "process start")
    }
}

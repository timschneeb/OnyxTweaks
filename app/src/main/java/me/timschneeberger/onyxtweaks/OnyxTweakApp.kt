package me.timschneeberger.onyxtweaks

import android.app.Application
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.utils.CustomLogger

// TODO add warning on other non-Go 7 models and non Android 12 devices
// TODO add hook issue detection and warning page

class OnyxTweakApp : Application() {
    override fun onCreate() {
        Log.currentLogger = CustomLogger
        EzXHelper.setLogTag("OnyxTweaksMgr")
        Log.d("OnyxTweaksApp initialized")
        super.onCreate()
    }
}
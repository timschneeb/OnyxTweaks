package me.timschneeberger.onyxtweaks

import android.app.Application
import android.os.Build
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.utils.CustomLogger
import me.timschneeberger.onyxtweaks.utils.onyxVersion

class OnyxTweakApp : Application() {
    override fun onCreate() {
        Log.currentLogger = CustomLogger
        EzXHelper.setLogTag("OnyxTweaksMgr")
        Log.d("OnyxTweaksApp initialized")
        Log.i("Device: ${Build.FINGERPRINT}; Detected version: $onyxVersion")
        super.onCreate()
    }
}
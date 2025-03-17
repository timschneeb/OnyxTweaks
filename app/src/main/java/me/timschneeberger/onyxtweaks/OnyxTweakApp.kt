package me.timschneeberger.onyxtweaks

import android.app.Application
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.utils.CustomLogger
import org.lsposed.hiddenapibypass.HiddenApiBypass

class OnyxTweakApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.currentLogger = CustomLogger
        EzXHelper.setLogTag("OnyxTweaksMgr")
        HiddenApiBypass.addHiddenApiExemptions("");

        Log.d("OnyxTweaksApp initialized")
    }

    fun get(): OnyxTweakApp? {
        if (instance == null)
            instance = OnyxTweakApp()
        return instance
    }

    companion object {
        private var instance: OnyxTweakApp? = null
    }
}
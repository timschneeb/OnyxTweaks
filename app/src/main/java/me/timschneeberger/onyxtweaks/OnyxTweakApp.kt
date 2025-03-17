package me.timschneeberger.onyxtweaks

import android.app.Application
import com.github.kyuubiran.ezxhelper.EzXHelper
import org.lsposed.hiddenapibypass.HiddenApiBypass

class OnyxTweakApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        EzXHelper.setLogTag("OnyxTweaksMgr")
        HiddenApiBypass.addHiddenApiExemptions("");
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
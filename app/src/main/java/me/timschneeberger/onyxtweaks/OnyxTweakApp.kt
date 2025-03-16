package me.timschneeberger.onyxtweaks

import android.app.Application
import org.lsposed.hiddenapibypass.HiddenApiBypass

class OnyxTweakApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

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
package me.timschneeberger.onyxtweaks

import android.app.Application

class OnyxTweakApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
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
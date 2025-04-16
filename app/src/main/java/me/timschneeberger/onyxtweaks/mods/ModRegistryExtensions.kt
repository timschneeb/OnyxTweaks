package me.timschneeberger.onyxtweaks.mods

import android.os.Build

object ModRegistryExtensions {
    val ModRegistry.testedModels: Array<String>
        get() = arrayOf("GoColor7")
    val ModRegistry.testedAndroidVersions: Array<Int>
        get() = arrayOf(Build.VERSION_CODES.S_V2)
}
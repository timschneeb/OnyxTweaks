package me.timschneeberger.onyxtweaks.mods.base

import android.os.Bundle
import me.timschneeberger.onyxtweaks.receiver.ModEvents

abstract class LifecycleBroadcastHook : ModPack() {
    fun broadcastOnHook(packageName: String) {
        sendBroadcast(ModEvents.HOOK_LOADED, Bundle().apply {
            putString(ARG_PACKAGE, packageName)
        })
    }

    companion object {
        const val ARG_PACKAGE = "package"
    }
}
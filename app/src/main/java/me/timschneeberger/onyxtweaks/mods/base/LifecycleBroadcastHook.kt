package me.timschneeberger.onyxtweaks.mods.base

import android.os.Bundle
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.bridge.ModEvents
import me.timschneeberger.onyxtweaks.bridge.ModEvents.Companion.ARG_PACKAGE
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

abstract class LifecycleBroadcastHook : ModPack() {
    override val group = PreferenceGroups.MISC

    final override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        sendEvent(ModEvents.HOOK_LOADED, Bundle().apply {
            putString(ARG_PACKAGE, lpParam.packageName)
        })
    }
}
package me.timschneeberger.onyxtweaks.mods.base

import android.os.Bundle
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.bridge.ModEvents
import me.timschneeberger.onyxtweaks.bridge.ModEvents.Companion.ARG_PACKAGE
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * Abstract hook that sends the HOOK_LOADED event when the package is loaded.
 * Extend this class and apply the [me.timschneeberger.onyxtweaks.mod_processor.TargetPackages] annotation to the class to use it.
 */
abstract class LifecycleBroadcastHook : ModPack() {
    final override val group = PreferenceGroups.NONE

    final override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        sendEvent(ModEvents.HOOK_LOADED, Bundle().apply {
            putString(ARG_PACKAGE, lpParam.packageName)
        })
    }
}
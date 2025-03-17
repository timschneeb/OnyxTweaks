package me.timschneeberger.onyxtweaks.mods.systemui

import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.LifecycleBroadcastHook
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(SYSTEM_UI_PACKAGE)
class SystemUiLifecycleHook : LifecycleBroadcastHook() {
    override val group = PreferenceGroups.MISC

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (lpParam.packageName != SYSTEM_UI_PACKAGE)
            return

        broadcastOnHook(SYSTEM_UI_PACKAGE)
    }
}
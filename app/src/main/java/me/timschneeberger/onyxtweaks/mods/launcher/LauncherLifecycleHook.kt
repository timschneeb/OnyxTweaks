package me.timschneeberger.onyxtweaks.mods.launcher

import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.LifecycleBroadcastHook
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(LAUNCHER_PACKAGE)
class LauncherLifecycleHook : LifecycleBroadcastHook() {
    override val group = PreferenceGroups.MISC

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (lpParam.packageName != LAUNCHER_PACKAGE)
            return

        broadcastOnHook(LAUNCHER_PACKAGE)
    }
}
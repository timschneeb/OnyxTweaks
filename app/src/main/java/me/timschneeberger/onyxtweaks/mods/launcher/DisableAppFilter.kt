package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.getClass
import me.timschneeberger.onyxtweaks.utils.replaceWithConstant

@TargetPackages(LAUNCHER_PACKAGE)
class DisableAppFilter : ModPack() {
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        // Note: added icons will persist after turning off this mod
        getClass("com.onyx.common.common.model.DeviceConfig").apply {
            methodFinder()
                .firstByName("getAppsFilter")
                .replaceWithConstant(listOf<String>(LAUNCHER_PACKAGE))
        }
    }
}
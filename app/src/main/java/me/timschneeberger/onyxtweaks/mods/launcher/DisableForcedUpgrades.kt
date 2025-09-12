package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion
import me.timschneeberger.onyxtweaks.utils.onyxVersion


/**
 * This mod pack prevents the Onyx Launcher from forcing system updates and disabling status & nav bar
 */
@TargetPackages(LAUNCHER_PACKAGE)
class DisableForcedUpgrades : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (onyxVersion < "4.1".toVersion()) return

        // Disable forced upgrade flag, to prevent the system getting locked down
        MethodFinder.fromClass("com.onyx.common.setting.model.OTABundle")
            .firstByName("isForceUpgrade")
            .replaceWithConstant (false)

        // Override comparison result of fingerprint with blacklisted ones
        val actionCls = findClass("com.onyx.common.setting.action.ForceUpgradeStateCheckAction")
        MethodFinder.fromClass(actionCls)
            .filterByParamTypes(actionCls, actionCls)
            .first()
            .replaceWithConstant (false)
    }
}
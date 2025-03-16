package me.timschneeberger.onyxtweaks.mods.shared

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.replaceWithConstant

@TargetPackages(SYSTEM_UI_PACKAGE, SYSTEM_FRAMEWORK_PACKAGE)
class RemoveRegalModeRestriction : ModPack() {
    override val group = PreferenceGroups.EINK

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_eink_center_always_show_regal_mode))
            return

        if (lpParam.packageName == SYSTEM_FRAMEWORK_PACKAGE) {
            MethodFinder.fromClass("android.onyx.optimization.data.v2.EACRefreshConfig")
                .firstByName("isSupportRegal")
                .replaceWithConstant(true)
        }
        else if (lpParam.packageName == SYSTEM_UI_PACKAGE) {
            MethodFinder.fromClass("com.android.systemui.settings.EInkCenterController")
                .firstByName("currentTopComponentSupportRegal")
                .replaceWithConstant(true)
        }
    }
}
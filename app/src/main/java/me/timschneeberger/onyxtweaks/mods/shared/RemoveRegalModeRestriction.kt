package me.timschneeberger.onyxtweaks.mods.shared

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceCatchingWithExpression
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * Disables the restriction of Regal mode in the Onyx EInk Center.
 */
@TargetPackages(SYSTEM_UI_PACKAGE, SYSTEM_FRAMEWORK_PACKAGE)
class RemoveRegalModeRestriction : ModPack() {
    override val group = PreferenceGroups.EINK

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (lpParam.packageName == SYSTEM_FRAMEWORK_PACKAGE) {
            MethodFinder.fromClass("android.onyx.optimization.data.v2.EACRefreshConfig")
                .firstByName("isSupportRegal")
                .replaceCatchingWithExpression<RemoveRegalModeRestriction> { preferences.get<Boolean>(R.string.key_eink_center_always_show_regal_mode) }
        }
        else if (lpParam.packageName == SYSTEM_UI_PACKAGE) {
            MethodFinder.fromClass("com.android.systemui.settings.EInkCenterController")
                .firstByName("currentTopComponentSupportRegal")
                .replaceCatchingWithExpression<RemoveRegalModeRestriction> { preferences.get<Boolean>(R.string.key_eink_center_always_show_regal_mode) }
        }
    }
}
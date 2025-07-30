package me.timschneeberger.onyxtweaks.mods.framework

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethodCatching
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * In Firmware v4.1 beta (2025-07-26_22-49_4.1-beta_0726_6aa3fa239),
 * the ActivityManagerService contains a NPE bug that causes a crash when starting Magisk.
 */
@TargetPackages(Constants.SYSTEM_FRAMEWORK_PACKAGE)
class PreventActivityServiceCrash : ModPack() {
    override val group = PreferenceGroups.NONE

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        findClass("com.android.server.am.ActivityManagerService")
            .methodFinder()
            .filterByName("addPackageDependency")
            .filterByParamTypes(String::class.java)
            .first()
            .createReplaceHookCatching<PreventActivityServiceCrash> { param ->
                // Handle any exception that occurs in the original method
                param.invokeOriginalMethodCatching()
            }
    }
}
package me.timschneeberger.onyxtweaks.mods.shared

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.replaceWithConstant

@TargetPackages(SYSTEM_UI_PACKAGE, SYSTEM_FRAMEWORK_PACKAGE)
class QuickTileGridSize : ModPack() {
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("android.onyx.systemui.SystemUIConfig")
            .firstByName("getQSNumColumns")
            .replaceWithConstant(4)
    }

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        if (param.packageName != SYSTEM_UI_PACKAGE)
            return

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "integer",
            "quick_settings_max_columns",
            4
        )

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "integer",
            "quick_settings_min_num_tiles",
            1
        )

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "integer",
            "quick_settings_min_rows",
            1
        )
    }
}
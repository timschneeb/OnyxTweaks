package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.replaceWithConstant

@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class QuickTileGridSize : ModPack() {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_qs_grid_custom_size))
            return

        MethodFinder.fromClass("android.onyx.systemui.SystemUIConfig")
            .firstByName("getQSNumColumns")
            .replaceWithConstant(4)
    }

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        if (!preferences.get<Boolean>(R.string.key_qs_grid_custom_size))
            return

        param.res.setReplacement(
            Constants.SYSTEM_UI_PACKAGE,
            "integer",
            "quick_settings_max_columns",
            preferences.get<Int>(R.string.key_qs_grid_column_count)
        )

        param.res.setReplacement(
            Constants.SYSTEM_UI_PACKAGE,
            "integer",
            "quick_settings_min_rows",
            preferences.get<Int>(R.string.key_qs_grid_row_count)
        )

        if (preferences.get<Boolean>(R.string.key_qs_grid_no_min_tile_count)) {
                param.res.setReplacement(
                    Constants.SYSTEM_UI_PACKAGE,
                    "integer",
                    "quick_settings_min_num_tiles",
                    1
                )
            }
    }
}
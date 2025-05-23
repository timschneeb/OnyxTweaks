package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.IResourceHook
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack allows the user to change the grid size of the quick settings tiles
 * and remove the minimum tile count.
 */
@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class QuickTileGridSize : ModPack(), IResourceHook {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_qs_grid_custom_size))
            return

        MethodFinder.fromClass("android.onyx.systemui.SystemUIConfig")
            .firstByName("getQSNumColumns")
            .replaceWithConstant(preferences.getStringAsInt(R.string.key_qs_grid_column_count))
    }

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        if (!preferences.get<Boolean>(R.string.key_qs_grid_custom_size))
            return

        param.res.setReplacement(
            Constants.SYSTEM_UI_PACKAGE,
            "integer",
            "quick_settings_max_columns",
            preferences.getStringAsInt(R.string.key_qs_grid_column_count)
        )

        param.res.setReplacement(
            Constants.SYSTEM_UI_PACKAGE,
            "integer",
            "quick_settings_min_rows",
            preferences.getStringAsInt(R.string.key_qs_grid_row_count)
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
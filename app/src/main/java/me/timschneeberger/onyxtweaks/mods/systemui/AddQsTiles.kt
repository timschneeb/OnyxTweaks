package me.timschneeberger.onyxtweaks.mods.systemui

import android.annotation.SuppressLint
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(SYSTEM_UI_PACKAGE)
class AddQsTiles : ModPack() {
    override val group = PreferenceGroups.QS

    @SuppressLint("DiscouragedApi")
    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        var defaultTiles = param.res.getIdentifier(
            "quick_settings_tiles_stock",
            "string",
            SYSTEM_UI_PACKAGE
        ).let { param.res.getString(it) }

        if (preferences.get<Boolean>(R.string.key_qs_grid_show_bw_tile))
            defaultTiles += ",bw_mode"

        if (preferences.get<Boolean>(R.string.key_qs_grid_show_split_screen_tile))
            defaultTiles += ",spilt_screen"

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "string",
            "quick_settings_tiles_stock",
            defaultTiles
        )
    }
}
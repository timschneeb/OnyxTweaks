package me.timschneeberger.onyxtweaks.mods.systemui

import android.annotation.SuppressLint
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(SYSTEM_UI_PACKAGE)
class AddGrayscaleModeQsTile : ModPack() {
    override val group = PreferenceGroups.QS

    @SuppressLint("DiscouragedApi")
    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        val defaultStringId = param.res.getIdentifier(
            "quick_settings_tiles_stock",
            "string",
            SYSTEM_UI_PACKAGE
        )

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "string",
            "quick_settings_tiles_stock",
            param.res.getString(defaultStringId) + ",bw_mode"
        )
    }
}
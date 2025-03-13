package me.timschneeberger.onyxtweaks.mods.systemui

import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages

@TargetPackages(SYSTEM_UI_PACKAGE)
class CompactQsPanel : ModPack() {
    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "bool",
            "qs_panel_height_custom",
            true
        )

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "bool",
            "qs_panel_custom_bg",
            true
        )
    }
}
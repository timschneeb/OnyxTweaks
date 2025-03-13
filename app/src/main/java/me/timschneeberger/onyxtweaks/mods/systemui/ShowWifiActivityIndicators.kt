package me.timschneeberger.onyxtweaks.mods.systemui

import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages

@TargetPackages(SYSTEM_UI_PACKAGE)
class ShowWifiActivityIndicators : ModPack() {
    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "bool",
            "config_showActivity",
            false
        )
    }
}
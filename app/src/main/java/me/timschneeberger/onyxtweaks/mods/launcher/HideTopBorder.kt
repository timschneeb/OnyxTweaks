package me.timschneeberger.onyxtweaks.mods.launcher

import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages

@TargetPackages(LAUNCHER_PACKAGE)
class HideTopBorder : ModPack() {
    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        param.res.setReplacement(
            LAUNCHER_PACKAGE,
            "color",
            "main_activity_top_border_color",
            android.R.color.transparent
        )
    }
}
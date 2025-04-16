package me.timschneeberger.onyxtweaks.mods.systemui

import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(SYSTEM_UI_PACKAGE)
class ShowWifiActivityIndicators : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "bool",
            "config_showActivity",
            preferences.get<Boolean>(R.string.key_status_bar_status_icons_show_wifi_activity)
        )
    }
}
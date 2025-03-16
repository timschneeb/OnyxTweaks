package me.timschneeberger.onyxtweaks.mods.systemui

import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(SYSTEM_UI_PACKAGE)
class CompactQsPanel : ModPack() {
    override val group = PreferenceGroups.QS

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        val enable = preferences.get<Boolean>(R.string.key_qs_panel_compact)

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "bool",
            "qs_panel_height_custom",
            enable
        )

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "bool",
            "qs_panel_custom_bg",
            enable
        )
    }
}
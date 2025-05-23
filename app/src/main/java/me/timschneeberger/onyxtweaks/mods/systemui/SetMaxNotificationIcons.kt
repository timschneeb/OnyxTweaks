package me.timschneeberger.onyxtweaks.mods.systemui

import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.IResourceHook
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack allows the user to set the maximum number of notification icons in the status bar.
 */
@TargetPackages(SYSTEM_UI_PACKAGE)
class SetMaxNotificationIcons : ModPack(), IResourceHook {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "integer",
            "onyx_notification_container_max_icons",
            preferences.getStringAsInt(R.string.key_status_bar_status_icons_max_notification_icons)
        )
    }
}
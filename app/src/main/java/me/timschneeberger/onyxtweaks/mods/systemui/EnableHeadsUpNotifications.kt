package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack enables heads-up notifications.
 *
 * Devices with a smartphone form factor (Onyx Palma) have allow heads-up notifications by default.
 */
@TargetPackages(SYSTEM_UI_PACKAGE)
class EnableHeadsUpNotifications : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        val enable = preferences.get<Boolean>(R.string.key_status_bar_notifications_heads_up)

        findClass("android.onyx.systemui.SystemUIConfig").apply {
            methodFinder()
                .firstByName("isDisableNotificationListenForHeadsUp")
                .replaceWithConstant(!enable)

            methodFinder()
                .firstByName("isDisableHeadsUpPinnedNotification")
                .replaceWithConstant(!enable)
        }
    }
}
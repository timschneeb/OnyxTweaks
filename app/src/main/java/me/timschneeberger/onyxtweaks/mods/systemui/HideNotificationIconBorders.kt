package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.mods.utils.firstByNameOrLog

@TargetPackages(SYSTEM_UI_PACKAGE)
class HideNotificationIconBorders : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_status_bar_status_icons_remove_borders))
            return

        MethodFinder.fromClass("com.android.systemui.statusbar.StatusBarIconView")
            .firstByNameOrLog("setNotification")
            .createAfterHook { param ->
                param.thisObject
                    .objectHelper()
                    .setObject("mNotifRoundPaint", null)
            }

        MethodFinder.fromClass("com.android.systemui.statusbar.phone.NotificationIconAreaController")
            .firstByNameOrLog("resetIconConfig")
            .createHook {
                // Bypass method
                replace { it.args.first() }
            }
    }
}
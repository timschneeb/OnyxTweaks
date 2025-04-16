package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(SYSTEM_UI_PACKAGE)
class HideNotificationIconBorders : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_status_bar_status_icons_remove_borders))
            return

        MethodFinder.fromClass("com.android.systemui.statusbar.StatusBarIconView")
            .firstByName("setNotification")
            .createAfterHookCatching<HideNotificationIconBorders> { param ->
                param.thisObject
                    .objectHelper()
                    .setObject("mNotifRoundPaint", null)
            }

        MethodFinder.fromClass("com.android.systemui.statusbar.phone.NotificationIconAreaController")
            .firstByName("resetIconConfig")
            .createReplaceHookCatching<HideNotificationIconBorders> {
                // Bypass method
                it.args.first()
            }
    }
}
package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.firstByName

@TargetPackages(SYSTEM_UI_PACKAGE)
class HideNotificationIconBorders : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("com.android.systemui.statusbar.StatusBarIconView")
            .firstByName("setNotification")
            .createAfterHook { param ->
                param.thisObject
                    .objectHelper()
                    .setObject("mNotifRoundPaint", null)
            }

        MethodFinder.fromClass("com.android.systemui.statusbar.phone.NotificationIconAreaController")
            .firstByName("resetIconConfig")
            .createHook {
                // Bypass method
                replace { it.args.first() }
            }
    }
}
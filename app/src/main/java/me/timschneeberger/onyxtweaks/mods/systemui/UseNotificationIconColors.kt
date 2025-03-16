package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.replaceWithConstant

@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class UseNotificationIconColors : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("android.onyx.systemui.SystemUIConfig")
            .firstByName("isFixNotificationIconColor")
            .replaceWithConstant(
                !preferences.get<Boolean>(R.string.key_status_bar_notifications_enable_icon_colors)
            )
    }
}
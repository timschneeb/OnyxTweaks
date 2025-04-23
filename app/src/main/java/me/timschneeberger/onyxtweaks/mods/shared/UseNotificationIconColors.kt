package me.timschneeberger.onyxtweaks.mods.shared

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceCatchingWithExpression
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack re-enables the coloring of the small notification app icons in the
 * pulled down notification shade.
 */
@TargetPackages(Constants.SYSTEM_UI_PACKAGE, Constants.SYSTEM_FRAMEWORK_PACKAGE)
class UseNotificationIconColors : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("android.onyx.systemui.SystemUIConfig")
            .firstByName("isFixNotificationIconColor")
            .replaceCatchingWithExpression<UseNotificationIconColors> {
                !preferences.get<Boolean>(R.string.key_status_bar_notifications_enable_icon_colors)
            }
    }
}
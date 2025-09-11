package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createBeforeHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion
import me.timschneeberger.onyxtweaks.utils.onyxVersion

/**
 * This mod pack shows additional Onyx-related status icons in the status bar.
 */
@TargetPackages(SYSTEM_UI_PACKAGE)
class ShowAdditionalStatusIcons : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl")
            .firstByName("setIconVisibility")
            .createBeforeHookCatching<ShowAdditionalStatusIcons> { param ->
                val str = param.args.firstOrNull() as? String
                when (str) {
                    // On v4.1+, the update mode icon doesn't correspond to the update mode anymore
                    "refresh_mode" -> param.args[1] = onyxVersion < "4.1".toVersion() && preferences.get<Boolean>(R.string.key_status_bar_status_icons_show_refresh_mode)
                    "tp_touch_mode" -> param.args[1] = preferences.get<Boolean>(R.string.key_status_bar_status_icons_show_touch_mode)
                }
            }
    }
}
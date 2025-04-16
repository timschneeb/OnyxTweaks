package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(SYSTEM_UI_PACKAGE)
class FixStatusBarIconColor : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_status_bar_status_icons_fix_notification_colors))
            return

        // Force-enable onyx color filter (normally only used on B/W-devices)
        findClass("com.android.systemui.statusbar.StatusBarIconView")
            .methodFinder()
            .firstByName("useOnyxColorFilter")
            .createReplaceHookCatching<FixStatusBarIconColor> {
                it.thisObject.objectHelper().getObjectOrNull("isBitmapDrawable") ?: false
            }

        // Treat everything as dark icons (this needs some testing)
        findClass("com.android.systemui.statusbar.StatusBarIconView")
            .methodFinder()
            .firstByName("isDarkPng")
            .replaceWithConstant(false)
    }
}
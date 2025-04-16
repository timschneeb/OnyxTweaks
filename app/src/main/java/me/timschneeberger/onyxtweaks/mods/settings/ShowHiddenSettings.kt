package me.timschneeberger.onyxtweaks.mods.settings

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_SETTINGS_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

typealias ResParam = XC_InitPackageResources.InitPackageResourcesParam

@TargetPackages(SYSTEM_SETTINGS_PACKAGE)
class ShowHiddenSettings : ModPack() {
    override val group = PreferenceGroups.SYSTEM_SETTINGS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (preferences.get<Boolean>(R.string.key_settings_show_gestures)) {
            findClass("com.android.settings.gestures.OneHandedSettingsUtils")
                .methodFinder()
                .firstByName("isSupportOneHandedMode")
                .replaceWithConstant(true)
            findClass("com.android.settings.gestures.OneHandedSettingsUtils")
                .methodFinder()
                .firstByName("canEnableController")
                .replaceWithConstant(true)

            findClass("com.android.settings.gestures.PowerMenuPreferenceController")
                .methodFinder()
                .firstByName("isAssistInvocationAvailable")
                .replaceWithConstant(true)
            findClass("com.android.settings.gestures.LongPressPowerButtonPreferenceController")
                .methodFinder()
                .firstByName("getAvailabilityStatus")
                .replaceWithConstant(0)
            findClass("com.android.settings.gestures.DoubleTapPowerPreferenceController")
                .methodFinder()
                .firstByName("isGestureAvailable")
                .replaceWithConstant(true)
        }

        if (preferences.get<Boolean>(R.string.key_settings_show_memory_in_app_info)) {
            findClass("com.android.settings.applications.appinfo.AppMemoryPreferenceController")
                .methodFinder()
                .firstByName("getAvailabilityStatus")
                .replaceWithConstant(0)
        }
    }
}
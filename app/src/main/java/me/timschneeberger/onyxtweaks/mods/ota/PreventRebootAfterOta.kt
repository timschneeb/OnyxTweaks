package me.timschneeberger.onyxtweaks.mods.ota

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.OTA_SERVICE_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


typealias ResParam = XC_InitPackageResources.InitPackageResourcesParam

/**
 * This mod pack prevents the device from rebooting automatically after an OTA update.
 *
 * This is useful to be able to re-install Magisk to the new boot image before the reboot.
 */
@TargetPackages(OTA_SERVICE_PACKAGE)
class ShowHiddenSettings : ModPack() {
    override val group = PreferenceGroups.MISC

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (preferences.get<Boolean>(R.string.key_misc_skip_reboot_after_ota)) {
            findClass("com.onyx.android.onyxotaservice.OnyxOtaService")
                .methodFinder()
                .firstByName("reboot")
                .replaceWithConstant(null)
        }
    }
}
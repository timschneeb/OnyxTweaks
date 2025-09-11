package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack show a hidden setting allowing users to change the location
 * of the function bar in the Onyx Launcher.
 *
 * The setting is only visible by default on large screen devices.
 */
@TargetPackages(LAUNCHER_PACKAGE)
class ChangeFunctionBarLocation : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("com.onyx.common.common.model.DeviceConfig")
            .firstByName("isSupportChangeFunctionBarLocation")
            .replaceWithConstant(preferences.get<Boolean>(R.string.key_launcher_bar_show_position_setting, reload = true))
    }
}
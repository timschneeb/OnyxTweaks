package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceCatchingWithExpression
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack enables the physical keyboard/mouse settings in the Onyx settings.
 */
@TargetPackages(LAUNCHER_PACKAGE)
class EnableKeyboardSettings : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("com.onyx.common.common.model.DeviceConfig")
            .firstByName("isEnableKeyboardSetting")
            .replaceCatchingWithExpression<EnableKeyboardSettings> { preferences.get<Boolean>(R.string.key_launcher_settings_show_physical_input, reload = true) }
    }
}
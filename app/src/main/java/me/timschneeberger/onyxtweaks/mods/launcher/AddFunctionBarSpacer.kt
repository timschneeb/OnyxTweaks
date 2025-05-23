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
 * This mod pack adds a spacer to the function bar in the Onyx Launcher, or disables it.
 *
 * The spacer is placed between the last and the second last item in the function bar.
 * The spacer is inserted by default on large screen devices.
 */
@TargetPackages(LAUNCHER_PACKAGE)
class AddFunctionBarSpacer : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if(preferences.get<String>(R.string.key_launcher_bar_item_alignment) == "default")
            return

        MethodFinder.fromClass("com.onyx.common.common.model.DeviceConfig")
            .firstByName("isConfigFunctionBarSpace")
            .replaceWithConstant (
                when(preferences.get<String>(R.string.key_launcher_bar_item_alignment)) {
                    "spacer" -> true
                    else -> false
                }
            )
    }
}
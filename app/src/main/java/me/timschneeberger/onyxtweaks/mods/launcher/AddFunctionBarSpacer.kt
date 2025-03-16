package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.replaceWithConstant

@TargetPackages(LAUNCHER_PACKAGE)
class AddFunctionBarSpacer : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("loading AddFunctionBarSpacer for ${lpParam.packageName}")
        val mode = preferences.get<String>(R.string.key_launcher_bar_item_alignment)
        XposedBridge.log("Function bar spacer mode: $mode")

        if(mode == "default")
            return

        MethodFinder.fromClass("com.onyx.common.common.model.DeviceConfig")
            .firstByName("isConfigFunctionBarSpace")
            .replaceWithConstant(
                when(preferences.get<String>(R.string.key_launcher_bar_item_alignment)) {
                    "spacer" -> true
                    else -> false
                }
            )
    }
}
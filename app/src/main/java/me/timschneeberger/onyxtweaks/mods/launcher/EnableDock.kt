package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.applyObjectHelper
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(LAUNCHER_PACKAGE)
class EnableDock : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_launcher_desktop_show_dock))
            return

        findClass("com.onyx.common.common.model.DeviceConfig").apply {
            methodFinder()
                .firstByName("getHotSeatApps")
                .createReplaceHookCatching { param ->
                    // Return a list with one app
                    // This will initialize an empty dock. If the list were empty, the dock would not be shown.
                    return@createReplaceHookCatching listOf(
                        ConstructorFinder.fromClass("com.onyx.android.sdk.data.AppDataInfo")
                            .filterByParamCount(0)
                            .first()
                            .newInstance()
                            .applyObjectHelper {
                                setObject("packageName", "com.android.vending")
                            }
                    )
                }
        }
    }
}
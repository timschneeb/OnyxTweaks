package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack enables the bottom app dock in the Onyx Launcher.
 *
 * It does this by filling the default dock app list with a single app (Play Store).
 */
@TargetPackages(LAUNCHER_PACKAGE)
class EnableDock : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_launcher_desktop_show_dock))
            return

        findClass("com.onyx.common.common.model.DeviceConfig").apply {
            methodFinder()
                .firstByName("getHotSeatApps")
                .createReplaceHookCatching<EnableDock> { param ->
                    // Return a list with one app
                    // This will initialize an empty dock. If the list were empty, the dock would not be shown.
                    return@createReplaceHookCatching listOf(
                        ConstructorFinder.fromClass("com.onyx.android.sdk.data.AppDataInfo")
                            .filterByParamCount(0)
                            .first()
                            .newInstance()
                            .objectHelper()
                            .setObject("packageName", "com.android.vending")
                    )
                }
        }
    }
}
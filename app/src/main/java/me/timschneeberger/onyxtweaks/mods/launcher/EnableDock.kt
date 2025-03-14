package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.getClass

@TargetPackages(LAUNCHER_PACKAGE)
class EnableDock : ModPack() {
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        getClass("com.onyx.common.common.model.DeviceConfig").apply {
            methodFinder()
                .firstByName("getHotSeatApps")
                .createHook {
                    replace { param ->
                        // Return a list with one dummy app
                        // This will initialize an empty dock. If the list were empty, the dock would not be shown.
                        return@replace listOf(
                            ConstructorFinder.fromClass("com.onyx.android.sdk.data.AppDataInfo")
                                .filterByParamCount(0)
                                .first()
                                .newInstance()
                                .objectHelper {
                                    setObject("packageName", "com.dummy.package")
                                    setObject("isEnable", false)
                                }
                        )
                    }
                }
        }
    }
}
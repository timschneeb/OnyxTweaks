package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
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
                .firstByName("getHotseatApps")
                .createHook {
                    replace { param ->
                        // TODO create dummy list instead
                        val list = param.thisObject.javaClass.getMethod("getConfigExtraApps")
                            .invoke(param.thisObject) as? List<*>
                        param.result = list
                    }
                }
        }
    }
}
package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.firstByName

@TargetPackages(SYSTEM_UI_PACKAGE)
class ShowAdditionalStatusIcons : ModPack() {
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl")
            .firstByName("setIconVisibility")
            .createBeforeHook { param ->
                val str = param.args.firstOrNull() as? String
                when (str) {
                    "refresh_mode" -> param.args[1] = true
                    "tp_touch_mode" -> param.args[1] = true
                }
            }
    }
}
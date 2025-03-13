package me.timschneeberger.onyxtweaks.mods.framework

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.replaceWithConstant

@TargetPackages(SYSTEM_FRAMEWORK_PACKAGE) // TODO check scope
class UseNotificationIconColors : ModPack() {
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass("android.onyx.systemui.SystemUIConfig")
            .firstByName("isFixNotificationIconColor")
            .replaceWithConstant(false)
    }
}
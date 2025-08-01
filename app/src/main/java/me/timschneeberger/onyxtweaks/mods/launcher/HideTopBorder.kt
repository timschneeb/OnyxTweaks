package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack hides the black top border of the Onyx Launcher separating
 * the status bar from the app grid.
 */
@TargetPackages(LAUNCHER_PACKAGE)
class HideTopBorder : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_launcher_desktop_hide_top_border))
            return

        val configCls = findClass("com.onyx.reader.apps.model.UserAppConfig")

        if (configCls.declaredMethods.any { it.name == "getMainActivityBorderColor" }) {
            // On 4.1+, use the new getter method
            configCls
                .methodFinder()
                .firstByName("getMainActivityBorderColor")
                .replaceWithConstant(android.R.color.transparent)
        }
        else {
            // On 4.0, the value is a public field
            configCls
                .constructorFinder()
                .first()
                .createAfterHookCatching<HideTopBorder> { param ->
                        param.thisObject.objectHelper()
                            .setObjectUntilSuperclass(
                                "mainActivityBorderColor",
                                android.R.color.transparent
                            )
                }
        }
    }
}
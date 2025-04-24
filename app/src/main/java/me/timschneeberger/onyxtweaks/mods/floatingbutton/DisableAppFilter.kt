package me.timschneeberger.onyxtweaks.mods.floatingbutton

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.FLOATING_BUTTON_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack disables the app filter for the 'Open app' action in the floating button app.
 */
@TargetPackages(FLOATING_BUTTON_PACKAGE)
class DisableAppFilter : ModPack() {
    override val group = PreferenceGroups.FLOATING_BUTTON

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if(!preferences.get<Boolean>(R.string.key_floating_button_show_all_apps))
            return

        findClass("com.onyx.floatingbutton.util.DeviceConfig").apply {
            methodFinder()
                .firstByName("getAppsFilter")
                .replaceWithConstant(listOf<String>())
        }
    }
}
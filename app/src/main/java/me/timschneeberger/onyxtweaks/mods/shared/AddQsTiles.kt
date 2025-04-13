package me.timschneeberger.onyxtweaks.mods.shared

import android.annotation.SuppressLint
import android.onyx.ViewUpdateHelper
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethod
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(Constants.SYSTEM_UI_PACKAGE, Constants.SYSTEM_FRAMEWORK_PACKAGE)
class AddQsTiles : ModPack() {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (hostPackageName != Constants.SYSTEM_FRAMEWORK_PACKAGE)
            return

        if (preferences.get<Boolean>(R.string.key_qs_grid_show_bw_tile)) {
            // On boot, restore persisted B/W mode state
            if (Settings.Global.getInt(appContext.contentResolver, "view_update_bw_mode", 0) == 1) {
                ViewUpdateHelper.setBWMode(1)
            }
        }

        // Prevent OECService from overriding B/W state during activity switching
        findClass("android.onyx.optimization.impl.EACBaseDisplayImpl")
            .methodFinder()
            .firstByName("applyBwMode")
            .createReplaceHookCatching<AddQsTiles> { param ->
                // Bypass hook if disabled
                if(!preferences.get<Boolean>(R.string.key_qs_grid_show_bw_tile))
                    param.invokeOriginalMethod()
            }
    }

    @SuppressLint("DiscouragedApi")
    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        if (hostPackageName != Constants.SYSTEM_UI_PACKAGE)
            return

        var defaultTiles = param.res.getIdentifier(
            "quick_settings_tiles_stock",
            "string",
            Constants.SYSTEM_UI_PACKAGE
        ).let { param.res.getString(it) }

        if (preferences.get<Boolean>(R.string.key_qs_grid_show_bw_tile))
            defaultTiles += ",bw_mode"

        if (preferences.get<Boolean>(R.string.key_qs_grid_show_split_screen_tile))
            defaultTiles += ",spilt_screen"

        param.res.setReplacement(
            Constants.SYSTEM_UI_PACKAGE,
            "string",
            "quick_settings_tiles_stock",
            defaultTiles
        )
    }
}
package me.timschneeberger.onyxtweaks.mods.systemui

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.IResourceHook
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack adds the B/W mode and split screen tiles to the quick settings panel.
 *
 * The B/W mode tile needs additional hooks to enable persistence, see [me.timschneeberger.onyxtweaks.mods.framework.FixBwModePersistence]
 */
@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class AddQsTiles : ModPack(), IResourceHook {
    override val group = PreferenceGroups.QS

    @SuppressLint("DiscouragedApi")
    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        var defaultTiles = param.res.getIdentifier(
            "quick_settings_tiles_stock",
            "string",
            Constants.SYSTEM_UI_PACKAGE
        ).let {
            if(it == 0) {
                // This only happens in the first zygote process (probably the 32-bit one?)
                // The secondary zygote process can resolve the resource.
                Log.ex("Failed to find quick_settings_tiles_stock string")
                return
            }

            param.res.getString(it)
        }

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
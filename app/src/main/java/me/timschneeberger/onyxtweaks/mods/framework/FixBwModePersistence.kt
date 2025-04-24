package me.timschneeberger.onyxtweaks.mods.framework

import android.onyx.ViewUpdateHelper
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethodCatching
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * The B/W mode tile needs additional hooks to enable persistence.
 * After boot, the B/W mode must manually be restored.
 * When switching activities, the B/W mode is reset by the default EAC display config,
 * but the QS tile remains enabled. The EAC B/W display config is be ignored.
 *
 * Some other mod packs depend on this pack, they are checked in [shouldApplyFix].
 */
@TargetPackages(Constants.SYSTEM_FRAMEWORK_PACKAGE)
class FixBwModePersistence : ModPack() {
    override val group = PreferenceGroups.QS

    private fun shouldApplyFix() =
        preferences.get<Boolean>(R.string.key_qs_grid_show_bw_tile) ||
                preferences.get<Boolean>(R.string.key_floating_button_show_bw_function)

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!shouldApplyFix())
            return

        // On boot, restore persisted B/W mode state
        if (Settings.Global.getInt(EzXHelper.appContext.contentResolver, "view_update_bw_mode", 0) == 1) {
            ViewUpdateHelper.setBWMode(1)
        }

        // Prevent OECService from overriding B/W state during activity switching
        findClass("android.onyx.optimization.impl.EACBaseDisplayImpl")
            .methodFinder()
            .firstByName("applyBwMode")
            .createReplaceHookCatching<FixBwModePersistence> { param ->
                // Bypass hook if disabled
                if(!preferences.get<Boolean>(R.string.key_qs_grid_show_bw_tile))
                    param.invokeOriginalMethodCatching()
            }
    }
}
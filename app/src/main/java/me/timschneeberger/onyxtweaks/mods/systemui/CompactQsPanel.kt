package me.timschneeberger.onyxtweaks.mods.systemui

import android.view.View
import androidx.core.view.updatePadding
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.IResourceHook
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.dpToPx
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion
import me.timschneeberger.onyxtweaks.utils.castNonNull
import me.timschneeberger.onyxtweaks.utils.onyxVersion

/**
 * This mod pack enables a compact Quick Settings panel normally
 * only found on large screen devices.
 * The height wraps the content instead of filling the screen vertically.
 */
@TargetPackages(SYSTEM_UI_PACKAGE)
class CompactQsPanel : ModPack(), IResourceHook {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (onyxVersion < "4.1".toVersion())
            return

        // On v4.1+, the QS menu is missing bottom padding when in compact mode.
        // This adds the padding back to the bottom of the QS panel.
        findClass("com.android.systemui.qs.QSPanel")
            .methodFinder()
            .firstByName("initialize")
            .createAfterHookCatching<HideQsComponents> { param ->
                param.thisObject.objectHelper()
                    .getObjectOrNull("controlCenterView")
                    .castNonNull<View>()
                    .updatePadding(bottom = appContext.dpToPx(16))
            }
    }


    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        val enable = preferences.get<Boolean>(R.string.key_qs_panel_compact)

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "bool",
            "qs_panel_height_custom",
            enable
        )

        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "bool",
            "qs_panel_custom_bg",
            enable
        )
    }
}
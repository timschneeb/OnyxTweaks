package me.timschneeberger.onyxtweaks.mods.launcher

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createBeforeHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast

/**
 * This mod pack hides the app labels in the Onyx Launcher.
 *
 * Useful for tighter grid sizes using [DesktopGridSize].
 */
@TargetPackages(LAUNCHER_PACKAGE)
class HideAppLabels : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    @SuppressLint("DiscouragedApi")
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if(!preferences.get<Boolean>(R.string.key_launcher_desktop_hide_labels))
            return

        findClass("com.onyx.android.sdk.utils.ViewUtils").apply {
            methodFinder()
                .filterByParamTypes(View::class.java, Boolean::class.java)
                .firstByName("setViewVisibleOrGone")
                .createBeforeHookCatching<HideAppLabels> { param ->
                    // We want to force set the visibility when the view is the TextView label in launcher_app_item
                    param.args[0].cast<View>()?.let {
                        if (it is TextView && it.id == appContext.resources.getIdentifier("name", "id", LAUNCHER_PACKAGE)) {
                            // Check caller
                            Throwable().stackTrace.any { element ->
                                element.className.contains("ItemViewFactory")
                            }.takeIf { it == true }?.let {
                                // Override the visibility
                                param.args[1] = false
                            }
                        }
                    }
                }
        }
    }
}
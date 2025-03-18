package me.timschneeberger.onyxtweaks.mods.launcher

import android.annotation.SuppressLint
import android.view.View
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createBeforeHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast

@TargetPackages(LAUNCHER_PACKAGE)
class ShowAppsToolbar : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    @SuppressLint("DiscouragedApi")
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if(preferences.get<String>(R.string.key_launcher_desktop_toolbar_state) == "default")
            return

        findClass("com.onyx.android.sdk.utils.ViewUtils").apply {
            methodFinder()
                .filterByParamTypes(View::class.java, Boolean::class.java)
                .firstByName("setViewVisibleOrGone")
                .createBeforeHookCatching { param ->
                    /* We want to force set the visibility when the view is the root of the title bar
                     * Once we are have a possible candidate, we will check the stack trace (expensive operation)
                     * to see if it is being called from initTitleBarView. */

                    param.args[0].cast<View>()?.let {
                        // Check id of root view
                        if (it.id == appContext.resources.getIdentifier("title_bar", "id", LAUNCHER_PACKAGE)) {
                            var isHit = Throwable().stackTrace.any { element ->
                                element.className.contains("AppsListFragment") && element.methodName == "initTitleBarView"
                            }

                            if (isHit) {
                                param.args[1] = preferences.get<String>(R.string.key_launcher_desktop_toolbar_state) == "visible"
                            }
                        }
                    }
                }
        }
    }
}
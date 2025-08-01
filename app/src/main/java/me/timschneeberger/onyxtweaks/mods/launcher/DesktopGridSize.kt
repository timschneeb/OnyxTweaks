package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.bridge.ModEvents
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createBeforeHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.hasCompanion
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethodCatching
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.mods.utils.requireCompanionObject
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack allows users to set a custom grid size for the desktop and dock
 * in the Onyx Launcher.
 *
 * Additionally, it allows users to reset the initialization flag of the launcher,
 * allowing the launcher to reset its layout. This is useful if the user lowers
 * the grid size and ends up with icons that are off the screen and inaccessible.
 */
@TargetPackages(LAUNCHER_PACKAGE)
class DesktopGridSize : ModPack() {
    private var isInitializing = true

    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (preferences.get<Boolean>(R.string.key_launcher_reinit_flag)) {
            Log.ix("Launcher reinit flag is set")
            hookInitializationFlag()
        }

        if (!preferences.get<Boolean>(R.string.key_launcher_desktop_grid_custom_size))
            return

        val columns = preferences.getStringAsInt(R.string.key_launcher_desktop_column_count)
        val rows = preferences.getStringAsInt(R.string.key_launcher_desktop_row_count)

        findClass("com.onyx.common.applications.model.AppSettings")
            .let {
                // On 4.1+ these methods were moved to the companion object
                if(it.hasCompanion()) it.requireCompanionObject() else it
            }
            .run {
                methodFinder()
                    .firstByName("getDesktopFixCol")
                    .replaceWithConstant(columns)
                methodFinder()
                    .firstByName("getDesktopFixRow")
                    .replaceWithConstant(rows)
            }

        findClass("com.onyx.common.applications.model.AppSettings")
            .run {

                methodFinder()
                    .firstByName("calDesktopColumnCount")
                    .replaceWithConstant(columns)
                methodFinder()
                    .firstByName("calDesktopRowCount")
                    .replaceWithConstant(rows)

                methodFinder()
                    .firstByName("getDesktopColumnCount")
                    .replaceWithConstant(columns)
                methodFinder()
                    .firstByName("getDesktopRowCount")
                    .replaceWithConstant(rows)

                methodFinder()
                    .firstByName("getDockColumnCount")
                    .replaceWithConstant(
                        preferences.getStringAsInt(R.string.key_launcher_desktop_dock_column_count)
                    )
            }
    }

    private fun hookInitializationFlag() {
        findClass("com.onyx.common.applications.model.AppSettings").apply {
            methodFinder()
                .firstByName("isAppInit")
                .createReplaceHookCatching<DesktopGridSize> { param ->
                    if (isInitializing) false else param.invokeOriginalMethodCatching()
                }

            methodFinder()
                .filterByParamTypes(Boolean::class.java)
                .firstByName("setAppInit")
                .createBeforeHookCatching<DesktopGridSize> { param ->
                    if(param.args[0] == true) {
                        Log.ix("Launcher initialization finished. Sending broadcast")
                        sendEvent(ModEvents.LAUNCHER_REINITIALIZED)
                        isInitializing = false
                    }
                }
        }
    }
}
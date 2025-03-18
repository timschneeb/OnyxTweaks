package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.getClass
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethod
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.receiver.ModEvents
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

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

        getClass("com.onyx.common.applications.model.AppSettings").apply {
            methodFinder()
                .firstByName("getDesktopFixCol")
                .replaceWithConstant(columns)
            methodFinder()
                .firstByName("getDesktopFixRow")
                .replaceWithConstant(rows)

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
        getClass("com.onyx.common.applications.model.AppSettings").apply {
            methodFinder()
                .firstByName("isAppInit")
                .createHook {
                    replace { param ->
                        if (isInitializing) {
                            return@replace false
                        }

                        return@replace param.invokeOriginalMethod()
                    }
                }

            methodFinder()
                .filterByParamTypes(Boolean::class.java)
                .firstByName("setAppInit")
                .createBeforeHook { param ->
                    if(param.args[0] == true) {
                        Log.ix("Launcher initialization finished. Sending broadcast")
                        sendBroadcast(ModEvents.LAUNCHER_REINITIALIZED)
                        isInitializing = false
                    }
                }
        }
    }
}
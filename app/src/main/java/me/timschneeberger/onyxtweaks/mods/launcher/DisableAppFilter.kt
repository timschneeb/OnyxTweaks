package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.getClass
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethod

@TargetPackages(LAUNCHER_PACKAGE)
class DisableAppFilter : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        val showSettings = preferences.get<Boolean>(R.string.key_launcher_app_show_settings)
        val showFiles = preferences.get<Boolean>(R.string.key_launcher_app_show_file_mgr)
        val showAll = preferences.get<Boolean>(R.string.key_launcher_app_show_all)

        listOf<String>(LAUNCHER_PACKAGE)

        // Note: added icons will persist after turning off this mod
        getClass("com.onyx.common.common.model.DeviceConfig").apply {
            methodFinder()
                .firstByName("getAppsFilter")
                .createHook {
                    replace { param ->
                        val filter = param
                            .invokeOriginalMethod()
                            .cast<List<String>>()
                            ?.toMutableList()
                            ?: mutableListOf()

                        if(showAll)
                            return@replace listOf<String>(LAUNCHER_PACKAGE)
                        if(showSettings)
                            filter.remove("com.android.settings")
                        if(showFiles)
                            filter.remove("com.android.documentsui")

                        XposedBridge.log("FILTER LIST: ${filter.joinToString()}")
                        return@replace filter
                    }
                }
        }
    }
}
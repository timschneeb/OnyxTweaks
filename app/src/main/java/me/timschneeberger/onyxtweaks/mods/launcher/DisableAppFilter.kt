package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethodCatching
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast

@TargetPackages(LAUNCHER_PACKAGE)
class DisableAppFilter : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        val showSettings = preferences.get<Boolean>(R.string.key_launcher_app_show_settings)
        val showFiles = preferences.get<Boolean>(R.string.key_launcher_app_show_file_mgr)
        val showAll = preferences.get<Boolean>(R.string.key_launcher_app_show_all)

        listOf<String>(LAUNCHER_PACKAGE)

        // Note: added icons will persist after turning off this mod
        findClass("com.onyx.common.common.model.DeviceConfig").apply {
            methodFinder()
                .firstByName("getAppsFilter")
                .createReplaceHookCatching<DisableAppFilter> hook@ { param ->
                    val filter = param
                        .invokeOriginalMethodCatching()
                        .cast<List<String>>()
                        ?.toMutableList()
                        ?: mutableListOf()

                    if(showAll)
                        return@hook listOf<String>(LAUNCHER_PACKAGE)
                    if(showSettings)
                        filter.remove("com.android.settings")
                    if(showFiles)
                        filter.remove("com.android.documentsui")

                    // Log.dx("Modified launcher app filter list: ${filter.joinToString()}")
                    return@hook filter
                }
        }
    }
}
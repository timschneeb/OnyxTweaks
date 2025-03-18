package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.firstByNameOrLog
import me.timschneeberger.onyxtweaks.mods.utils.getClass
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(LAUNCHER_PACKAGE)
class HideFunctionBarItems : ModPack() {
    private data class FunctionItem(val name: String, val icon: String)
    private val hiddenItems get() =
        preferences.get<Set<String>>(R.string.key_launcher_bar_hidden_items)
            .map { it.split(";") }
            .map { FunctionItem(it[0], it[1]) }

    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (hiddenItems.isEmpty())
            return

        MethodFinder.fromClass("com.onyx.common.common.model.DeviceConfig")
            .firstByNameOrLog("getFunctionConfig")
            .createAfterHook { param ->
                val categoryCls = getClass("com.onyx.reader.main.model.FunctionConfig")
                categoryCls
                    .getMethod("getItemList")
                    .invoke(param.result)
                    .let { (it as List<*>).toMutableList() }
                    .apply {
                        removeIf { t ->
                            t?.objectHelper()?.getObjectOrNull("name") in hiddenItems.map(FunctionItem::name)
                        }
                    }
                    .also {
                        categoryCls
                            .methodFinder()
                            .firstByNameOrLog("setItemList")
                            .invoke(param.result, it)

                    }
            }
    }
}
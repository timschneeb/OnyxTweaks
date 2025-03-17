package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.ClassHelper.Companion.classHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.getClass
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(LAUNCHER_PACKAGE)
class HideFunctionBarItems : ModPack() {
    private data class FunctionItem(val name: String, val icon: String)
    private val injectedItems get() =
        preferences.get<Set<String>>(R.string.key_launcher_bar_hidden_items)
            .map { it.split(";") }
            .map { FunctionItem(it[0], it[1]) }

    private fun verifyContainerFunctionItem(item: String): Boolean =
        getClass("com.onyx.reader.main.model.FunctionConfig\$Function")
            .methodFinder()
            .filterStatic()
            .firstByName("isValid")
            .invoke(null, item)
            .let { it as? Boolean == true }
            .also {
                if (!it) {
                    Log.ex("Critical: HideFunctionBarItems: Invalid function item for container: $item")
                }
            }

    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (injectedItems.isEmpty())
            return

        MethodFinder.fromClass("com.onyx.common.common.model.DeviceConfig")
            .firstByName("getFunctionConfig")
            .createAfterHook { param ->
                val categoryCls = getClass("com.onyx.reader.main.model.FunctionConfig");
                categoryCls
                    .getMethod("getItemList")
                    .invoke(param.result)
                    .let { (it as List<*>).toMutableList() }
                    .also(MutableList<Any?>::clear)
                    .apply {
                        injectedItems
                            .mapNotNull(::createFunctionItem)
                            .forEach(this::add)
                    }
                    .also {
                        categoryCls
                            .methodFinder()
                            .firstByName("setItemList")
                            .invoke(param.result, it)

                    }
            }
    }

    private fun createFunctionItem(category: FunctionItem): Any? {
        if (!verifyContainerFunctionItem(category.name)) {
            Log.ex("C/HideFunctionBarItems: Invalid function item for container: $category")
            return null
        }

        return getClass("com.onyx.reader.main.model.FunctionConfig\$ConfigItem")
            .classHelper()
            .newInstance()
            .apply {
                objectHelper().run {
                    setObject("name", category.name)
                    setObject("image", category.icon)
                }
            }
    }
}
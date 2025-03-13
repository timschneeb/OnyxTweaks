package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.ClassHelper.Companion.classHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.getClass

@TargetPackages(LAUNCHER_PACKAGE)
class HideFunctionBarItems : ModPack() {
    private data class FunctionItem(val name: String, val icon: String)

    private val injectedCategories = listOf(
        FunctionItem("library", "home_library"),
        // FunctionItem("shop", "home_shop"),
        // FunctionItem("note", "home_note"),
        FunctionItem("storage", "home_storage"),
        FunctionItem("apps", "home_apps"),
        FunctionItem("setting", "home_setting"),
    )

    private fun verifyContainerFunctionItem(item: String): Boolean =
        getClass("com.onyx.reader.main.model.FunctionConfig\$Function")
            .methodFinder()
            .filterStatic()
            .firstByName("isValid")
            .invoke(null, item)
            .let { it as? Boolean == true }
            .also {

                if (!it) {
                    XposedBridge.log("Critical: HideFunctionBarItems: Invalid function item for container: $item")
                }
            }

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
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
                        injectedCategories
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
            XposedBridge.log("C/HideFunctionBarItems: Invalid function item for container: $category")
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
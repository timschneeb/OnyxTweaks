package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethod
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
            .firstByName("getFunctionConfig")
            .createAfterHookCatching<HideFunctionBarItems> { param ->
                val categoryCls = findClass("com.onyx.reader.main.model.FunctionConfig")
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
                            .firstByName("setItemList")
                            .invoke(param.result, it)
                    }
            }

        MethodFinder.fromClass("com.onyx.reader.main.adapter.FunctionBarAdapter")
            .firstByName("getItemViewType")
            .createReplaceHookCatching<HideFunctionBarItems> { param ->
                // WORKAROUND: When in side bar mode, the adapter can cause an OutOfRange exception when enumerating the items in the single column
                return@createReplaceHookCatching try {
                    param.invokeOriginalMethod()
                }
                catch (ex: IndexOutOfBoundsException) {
                    // Ignore and return the item id for an empty space
                    1
                }
            }
    }
}
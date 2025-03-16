package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.ClassHelper.Companion.classHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.getClass

@TargetPackages(LAUNCHER_PACKAGE)
class AddSettingCategories : ModPack() {
    private data class SettingCategory(val title: String, val name: String, val icon: String)
    private val injectedCategories get() =
        preferences.get<Set<String>>(R.string.key_launcher_settings_added_shortcuts)
            .map { it.split(";") }
            .map { SettingCategory(it[0], it[1], it[2]) }

    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (injectedCategories.isEmpty())
            return

        MethodFinder.fromClass("com.onyx.common.common.model.DeviceConfig")
            .firstByName("getSettingCategory")
            .createAfterHook { param ->
                val categoryCls = getClass("com.onyx.android.sdk.kcb.setting.model.SettingCategory");
                categoryCls
                    .getMethod("getItemList")
                    .invoke(param.result)
                    .let { (it as List<*>).toMutableList() }
                    .apply {
                        injectedCategories
                            .takeWhile { category ->
                                this.none { item ->
                                    item?.objectHelper()?.getObjectOrNull("name") == category.name
                                }
                            }
                            .map(::createSettingsEntry)
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

    private fun createSettingsEntry(category: SettingCategory): Any {
        return getClass("com.onyx.android.sdk.kcb.setting.model.SettingCategory\$ConfigItem")
            .classHelper()
            .newInstance()
            .apply {
                objectHelper().run {
                    setObject("title", category.title)
                    setObject("name", category.name)
                    setObject("image", category.icon)
                }
            }
    }
}
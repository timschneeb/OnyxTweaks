package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.ClassHelper.Companion.classHelper
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.hasField
import me.timschneeberger.onyxtweaks.mods.utils.hasFields
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack adds custom settings shortcuts to the launcher settings menu.
 *
 * It is especially useful for adding shortcuts to the app/freeze settings that become
 * inaccessible when enabling the app dock and widget feature, which hides the toolbar by default.
 */
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
            .createAfterHookCatching<AddSettingCategories> { param ->
                val categoryCls = findClass("com.onyx.android.sdk.kcb.setting.model.SettingCategory")
                categoryCls
                    .getMethod("getItemList")
                    .invoke(param.result)
                    .let { (it as List<*>).toMutableList() }
                    .apply {
                        injectedCategories
                            .takeWhile { category ->
                                this.none { item ->
                                    item ?: return@none false

                                    // On 4.0, this is a regular Java class with a public field and no getters
                                    if(item.javaClass.hasField("name")) {
                                        item.objectHelper().getObjectOrNull("name") == category.name
                                    }
                                    // On 4.1+, this is a Kotlin class with getters, but erased private member names
                                    else {
                                        item.javaClass
                                            .methodFinder()
                                            .firstByName("getName")
                                            .invoke(item) == category.name
                                    }
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

    private fun createSettingsEntry(category: SettingCategory) =
        findClass("com.onyx.android.sdk.kcb.setting.model.SettingCategory\$ConfigItem")
            .classHelper()
            .newInstance()
            .apply {
                // On 4.0 access fields directly
                if(javaClass.hasFields("name", "name", "image")) {
                    objectHelper {
                        setObject("title", category.title)
                        setObject("name", category.name)
                        setObject("image", category.icon)
                    }
                }
                else {
                    // On 4.1+ use setters instead
                    javaClass.run {
                        methodFinder().firstByName("setTitle").invoke(this@apply, category.title)
                        methodFinder().firstByName("setName").invoke(this@apply, category.name)
                        methodFinder().firstByName("setImage").invoke(this@apply, category.icon)
                    }
                }
            }
}
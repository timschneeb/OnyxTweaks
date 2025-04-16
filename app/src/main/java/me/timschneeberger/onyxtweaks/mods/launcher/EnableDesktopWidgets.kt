package me.timschneeberger.onyxtweaks.mods.launcher

import android.appwidget.AppWidgetProviderInfo
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
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
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast
import me.timschneeberger.onyxtweaks.utils.castNonNull
import java.lang.reflect.Modifier

@TargetPackages(LAUNCHER_PACKAGE)
class EnableDesktopWidgets : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if(!preferences.get<Boolean>(R.string.key_launcher_desktop_widgets_advanced))
            return

        findClass("com.onyx.common.common.model.DeviceConfig").apply {
            methodFinder()
                .firstByName("isEnableDesktopWidget")
                .replaceWithConstant(true)

            methodFinder()
                .firstByName("getFilterWidgets")
                .replaceWithConstant(emptyList<String>())

            methodFinder()
                .filter { name == "getDefaultWidgets" || name == "getConfigWidgets" }
                .forEach { method ->
                    method.replaceWithConstant(
                        mapOf(
                            -1 to "com.onyx.common.applications.appwidget.widget.QuickLauncherProvider",
                            -2 to "com.onyx.common.applications.appwidget.widget.LibraryRecentlyReadProvider",
                            -3 to "com.onyx.common.applications.appwidget.widget.ShopRecommendProvider",
                            -4 to "com.onyx.mail.calendar.widget.CurrentDayMemoWidgetProvider",
                            -6 to "com.onyx.common.applications.appwidget.widget.StatisticsWidgetProvider"
                        )
                    )
                }
        }

        // Unlock all widgets on the widget page
        MethodFinder.fromClass("com.onyx.common.applications.appwidget.action.LoadSettingWidgetModelsAction")
            .filterByParamCount(0)
            .filterByReturnType(List::class.java)
            .filterByModifiers(Modifier.PRIVATE)
            .first()
            .createReplaceHookCatching<EnableDesktopWidgets> hook@ { param ->
                // Get installed widgets
                val widgets = ArrayList<Any?>()
                val installedWidgets =
                    MethodFinder.fromClass("com.onyx.common.applications.appwidget.utils.AppWidgetUtils")
                        .firstByName("getInstalledWidgets")
                        .invoke(null)
                        .castNonNull<List<AppWidgetProviderInfo>>()

                for (appWidgetProviderInfo in installedWidgets) {
                    val viewModel =
                        ConstructorFinder.fromClass("com.onyx.common.applications.appwidget.model.AppWidgetItemViewModel")
                            .filterByParamTypes(AppWidgetProviderInfo::class.java)
                            .first()
                            .newInstance(appWidgetProviderInfo)
                            .apply {
                                // Invoke getters once to populate cache beforehand
                                MethodFinder.fromClass(this::class)
                                    .filter { name == "getLabel" || name == "getIcon" }
                                    .forEach { it.invoke(this) }
                            }

                    val widgetMap = MethodFinder.fromClass("com.onyx.common.applications.appwidget.model.AppWidgetBundle")
                        .filterStatic()
                        .firstByName("getInstance")
                        .invoke(null)
                        .run {
                            // Retrieve widgets map from AppWidgetBundle
                            MethodFinder.fromClass(this::class)
                                .firstByName("getWidgets")
                                .invoke(this)
                                .castNonNull<LinkedHashMap<*,*>>()
                        }

                    // Get provider string
                    val provider = appWidgetProviderInfo.provider.flattenToString()

                    // Attach widget id if available
                    widgetMap
                        .filter { entry -> entry.value == provider }
                        .forEach { entry ->
                            MethodFinder.fromClass(viewModel::class)
                                .firstByName("setAppWidgetId")
                                .invoke(
                                    viewModel,
                                    entry.key
                                )
                        }
                    widgets.add(viewModel)
                }

                return@hook widgets
            }

        // Force re-initialization if the widget page is empty
        MethodFinder.fromClass("com.onyx.common.applications.appwidget.utils.AppWidgetUtils")
            .firstByName("getSecondaryScreenWidgetsJsonFromMMKV")
            .createAfterHookCatching<EnableDesktopWidgets> { param ->
                param.result.cast<String>().takeIf {
                    it.isNullOrEmpty() || it.trim() == "[]" || it.trim() == "{}"
                }?.let {
                    param.result = null
                }
            }


        // Add provider name to widget label to distinguish between widgets with the same label
        MethodFinder.fromClass("com.onyx.common.applications.appwidget.model.AppWidgetSettingsItemLayoutModel")
            .firstByName("getLabel")
            .createAfterHookCatching<EnableDesktopWidgets> { param ->
                val label = param.result.castNonNull<String>()
                param.thisObject.javaClass
                    .methodFinder()
                    .firstByName("getInfo")
                    .invoke(param.thisObject)
                    .castNonNull<AppWidgetProviderInfo>()
                    .takeIf { info -> info.provider.packageName.contains("com.onyx") }
                    ?.let { info ->
                        param.result = String.format(
                            "%s (%s)",
                            label,
                            // Short provider name
                            info.provider.className
                                .split("\\.".toRegex())
                                .dropLastWhile(String::isEmpty)
                                .toTypedArray()
                                .takeIf { it.isNotEmpty() }
                                ?.last()
                                ?.replace("Provider", "") ?: "builtin"
                        )
                    }
            }
    }
}
package me.timschneeberger.onyxtweaks.mods.launcher

import android.annotation.SuppressLint
import android.view.View
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.dpToPx
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.runSafely
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * This mod pack shows the hidden settings button in the launcher desktop options menu.
 *
 * It will show open the launcher settings dialog when clicked.
 * The setting is only visible by default on large screen devices.
 */
@TargetPackages(LAUNCHER_PACKAGE)
class AddLauncherSettingsMenu : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    private class SettingsClickHandler(private val instances: MutableList<WeakReference<Any?>>) :
        InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any {
            instances
                .mapNotNull(WeakReference<Any?>::get)
                .forEach { instance: Any? ->
                    runSafely(AddLauncherSettingsMenu::class, "Failed to open launcher settings") {
                        val listenerCls = findClass("com.onyx.common.applications.view.DesktopOptionView")
                            .methodFinder()
                            .firstByName("getDesktopOptionViewListener")
                            .invoke(instance)

                        listenerCls.javaClass.getDeclaredMethod("onLaunchSettings").invoke(listenerCls)
                    }
                }

            instances.removeIf { it.get() == null }
            return true
        }
    }

    private val dovInstances = mutableListOf<WeakReference<Any?>>()

    @SuppressLint("DiscouragedApi")
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_launcher_desktop_show_settings)) {
            return
        }

        val desktopOptViewCls = findClass("com.onyx.common.applications.view.DesktopOptionView")
        val fastAdapterClickListenerCls = findClass("com.mikepenz.fastadapter.listeners.OnClickListener")
        val onClickMtd = desktopOptViewCls.methodFinder()
            .filterByParamTypes(fastAdapterClickListenerCls)
            .filterVoidReturnType()
            .first()

        HookFactory.createAfterHooks(
            ctors = desktopOptViewCls.constructors
        ) { param ->
            dovInstances.add(WeakReference(param.thisObject))
        }

        val proxy = Proxy.newProxyInstance(
            lpParam.classLoader,
            arrayOf(fastAdapterClickListenerCls),
            SettingsClickHandler(dovInstances)
        )

        onClickMtd.createAfterHookCatching<AddLauncherSettingsMenu> { param ->
            val fastAdapterCls = findClass("com.mikepenz.fastadapter.commons.adapters.FastItemAdapter")
            val arrayFastAdapter = java.lang.reflect.Array.newInstance(fastAdapterCls, 0).javaClass

            desktopOptViewCls.fieldFinder()
                .filterByType(arrayFastAdapter)
                .first()
                .get(param.thisObject)
                .let { it as Array<*> }
                .let { adapters ->

                    val items = fastAdapterCls
                        .getMethod("getAdapterItems")
                        .invoke(adapters[1]) as List<*>

                    val createItemMtd = desktopOptViewCls.methodFinder()
                        .filterByParamTypes(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, fastAdapterClickListenerCls)
                        .first()

                    val newItemCount = items.size + 1
                    val padding = appContext.resources.getIdentifier("desktop_option_bottom_item_horizontal_padding", "dimen", LAUNCHER_PACKAGE)
                        .takeIf { i -> i != 0 }
                        ?.let(appContext.resources::getDimensionPixelSize)
                        ?: appContext.dpToPx(25)

                    val oldWidth = MethodFinder.fromClass(View::class)
                        .filterByName("getWidth")
                        .first()
                        .invoke(param.thisObject) as Int

                    val newWidth = (oldWidth - (padding * (newItemCount - 1))) / newItemCount
                    items.forEach { item ->
                        item ?: return@forEach
                        item.javaClass
                            .methodFinder()
                            .firstByName("setWidth")
                            .invoke(item, newWidth)
                    }

                    val settingsItem = createItemMtd.invoke(
                        param.thisObject,
                        appContext.resources.getIdentifier("ic_setting_vector", "drawable", LAUNCHER_PACKAGE),
                        appContext.resources.getIdentifier("settings", "string", LAUNCHER_PACKAGE),
                        newWidth,
                        proxy
                    )
                    fastAdapterCls.getMethod("add", MutableList::class.java).invoke(adapters[1], listOf(settingsItem))
                }
        }
    }
}
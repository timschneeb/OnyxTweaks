package me.timschneeberger.onyxtweaks

import android.util.Log
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class LauncherSettingsClickHandler(private val instances: MutableList<WeakReference<Any?>>) :
    InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any {
        instances
            .mapNotNull(WeakReference<Any?>::get)
            .forEach { instance: Any? ->
                try {
                    val dovCls = XposedHelpers.findClass(
                        "com.onyx.common.applications.view.DesktopOptionView",
                        instance!!.javaClass.classLoader
                    )
                    val listener = dovCls.getDeclaredMethod("getDesktopOptionViewListener")
                        .invoke(instance)

                    listener.javaClass.getDeclaredMethod("onLaunchSettings").invoke(listener)
                    Log.e("LSPosed-ext", "D/ => Settings icon clicked +$instance")
                } catch (e: Exception) {
                    XposedBridge.log(e)
                }
            }

        instances.removeIf { it.get() == null }
        return true
    }
}
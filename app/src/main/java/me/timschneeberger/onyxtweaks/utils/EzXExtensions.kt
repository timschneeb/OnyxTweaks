package me.timschneeberger.onyxtweaks.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ClassFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

fun ClassFinder.Static.of(className: String): ClassFinder? {
    return try {
        of(Class.forName(className, false, EzXHelper.safeClassLoader))
    }
    catch (e: ClassNotFoundException) {
        XposedBridge.log("Class not found: $className")
        null
    }
}

fun getClass(className: String): Class<*> {
    return try {
        XposedHelpers.findClass(className, EzXHelper.classLoader)
        // Class.forName(className, false, EzXHelper.classLoader)
    }
    catch (e: ClassNotFoundException) {
        XposedBridge.log("[${EzXHelper.appContext.packageName}] Class not found: $className")

        throw e
    }
}

fun <T> Method.replaceWithConstant(value: T?) {
    createHook {
        replace { _ -> value }
    }
}

inline fun <T> T.applyObjectHelper(block: ObjectHelper.() -> Unit): T {
    this?.objectHelper(block)
    return this
}

fun MethodFinder.firstByName(name: String) = filterByName(name).first()

@SuppressLint("DiscouragedApi")
fun Resources.getResourceIdByName(name: String, type: String, packageName: String? = null) =
    getIdentifier(name, type, packageName ?: EzXHelper.hostPackageName).let { drawableId ->
        if (drawableId == 0) {
            XposedBridge.log("Resource $type/$name not found in $packageName")
            null
        }
        else {
            drawableId
        }
    }

fun Resources.getDrawableByName(name: String, packageName: String? = null) =
    getResourceIdByName(name, "drawable", packageName)?.let { drawableId ->
        ResourcesCompat.getDrawable(this, drawableId, null)
    }

fun Context.inflateLayoutByName(root: ViewGroup?, name: String, packageName: String? = null) =
    resources.getResourceIdByName(name, "layout", packageName)?.let { layoutId ->
        LayoutInflater.from(this).inflate(layoutId, root, false)
    }

fun Resources.getDimensionPxByName(name: String, packageName: String? = null) =
    getResourceIdByName(name, "dimen", packageName)?.let { dimenId ->
        getDimensionPixelSize(dimenId)
    }

fun XC_MethodHook.MethodHookParam.invokeOriginalMethod(): Any? {
    return try {
        XposedBridge.invokeOriginalMethod(this.method, this.thisObject, this.args)
    }
    catch (e: Exception) {
        XposedBridge.log("Error calling original method: ${e.message}")
        null
    }
}

fun runSafely(block: () -> Unit) {
    try {
        block()
    }
    catch (e: Exception) {
        XposedBridge.log(e)
    }
}

fun runSafelySilent(block: () -> Unit) {
    try {
        block()
    }
    catch (_: Exception) { }
}
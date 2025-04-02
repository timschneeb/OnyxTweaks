package me.timschneeberger.onyxtweaks.mods.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.typeOf

fun findClass(className: String): Class<*> {
    return try {
        if (!EzXHelper.isClassLoaderInited) {
            Log.wx("ClassLoader not yet ready, using system class loader")
        }

        XposedHelpers.findClass(className, EzXHelper.safeClassLoader)
    }
    catch (e: ClassNotFoundException) {
        Log.ex("Class not found: $className")
        throw e
    }
}

fun Method.createBeforeHookCatching(block: (XC_MethodHook.MethodHookParam) -> Unit) =
    createHook { before { it.runSafely(block) } }

fun Method.createAfterHookCatching(block: (XC_MethodHook.MethodHookParam) -> Unit) =
    createHook { after { it.runSafely(block) } }

fun Method.createReplaceHookCatching(block: (XC_MethodHook.MethodHookParam) -> Any?) =
    createHook { replace { it.runSafely(block) } }

fun <T> Constructor<T>.createBeforeHookCatching(block: (XC_MethodHook.MethodHookParam) -> Unit) =
    createHook { before { it.runSafely(block) } }

fun <T> Constructor<T>.createAfterHookCatching(block: (XC_MethodHook.MethodHookParam) -> Unit) =
    createHook { after { it.runSafely(block) } }

fun <T> Constructor<T>.createReplaceHookCatching(block: (XC_MethodHook.MethodHookParam) -> Any?) =
    createHook { replace { it.runSafely(block) } }

fun <T> Method.replaceWithConstant(value: T?) {
    createHook {
        replace { _ -> value }
    }
}

inline fun <reified T> Method.replaceCatchingWithExpression(crossinline block: () -> T) {
    createHook {
        replace { _ ->
            runSafely {
                block()
            }
        }
    }
}

inline fun <T> T.applyObjectHelper(block: ObjectHelper.() -> Unit): T {
    this?.objectHelper(block)
    return this
}

fun MethodFinder.firstByName(name: String): Method {
    return filterByName(name).first()
}

@SuppressLint("DiscouragedApi")
fun Resources.getResourceIdByName(name: String, type: String, packageName: String? = null) =
    getIdentifier(name, type, packageName ?: EzXHelper.hostPackageName).let { drawableId ->
        if (drawableId == 0) {
            Log.ex("Resource $type/$name not found in $packageName")
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
        Log.ex("Error calling original method '${method.name}': ${e.message}")
        null
    }
}

inline fun <T,reified TRet> T.runSafely(block: T.() -> TRet): TRet {
    try {
        return block()
    }
    catch (e: Exception) {
        Log.ex(e)

        // Throw if a return type is expected
        if (TRet::class != typeOf<Unit>()) {
            throw e
        }
    }

    // If no return type is expected, return null
    return null as TRet
}
package me.timschneeberger.onyxtweaks.mods.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.appContextNullable
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KClass

typealias MethodParam = XC_MethodHook.MethodHookParam

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

inline fun <reified TCaller : ModPack> Method.createBeforeHookCatching(noinline block: (MethodParam) -> Unit) =
    createHook { before { it.runHookSafely(TCaller::class, block) } }

inline fun <reified TCaller : ModPack> Method.createAfterHookCatching(noinline block: (MethodParam) -> Unit) =
    createHook { after { it.runHookSafely(TCaller::class, block) } }

inline fun <reified TCaller : ModPack> Method.createReplaceHookCatching(noinline block: (MethodParam) -> Any?) =
    createHook { replace { it.runHookSafely(TCaller::class, block) } }

inline fun <reified TCaller : ModPack> Constructor<*>.createBeforeHookCatching(noinline block: (MethodParam) -> Unit) =
    createHook { before { it.runHookSafely(TCaller::class, block) } }

inline fun <reified TCaller : ModPack> Constructor<*>.createAfterHookCatching(noinline block: (MethodParam) -> Unit) =
    createHook { after { it.runHookSafely(TCaller::class, block) } }

inline fun <reified TCaller : ModPack> Constructor<*>.createReplaceHookCatching(noinline block: (MethodParam) -> Any?) =
    createHook { replace { it.runHookSafely(TCaller::class, block) } }

inline fun <reified TCaller : ModPack> Method.replaceCatchingWithExpression(noinline block: () -> Any?) {
    createHook {
        replace { it ->
            it.runHookSafely(TCaller::class) { block() }
        }
    }
}

fun Method.replaceWithConstant(value: Any?) {
    createHook {
        replace { _ -> value }
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
    getIdentifier(name, type, packageName ?: hostPackageName).let { drawableId ->
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

fun MethodParam.invokeOriginalMethod(): Any? {
    return try {
        XposedBridge.invokeOriginalMethod(this.method, this.thisObject, this.args)
    }
    catch (e: Exception) {
        Log.ex("Error calling original method '${method.name}': ${e.message}")
        null
    }
}

fun <TRet> MethodParam.runHookSafely(caller: KClass<*>, block: MethodParam.() -> TRet): TRet? {
    try {
        return block(this)
    }
    catch (e: Exception) {
        Log.ex(e, "Exception in hooked method '${method.name}' of class '${method.declaringClass.name}' within mod pack '${caller.simpleName}'")
        appContextNullable?.sendHookExceptionEvent(e, null, method, callerClass = caller)
        return null!!
    }
}

fun <T,TRet> T.runSafely(caller: KClass<*>, message: String, isWarning: Boolean = false, block: T.() -> TRet): TRet? {
    try {
        return block(this)
    }
    catch (e: Exception) {
        Log.ex(e, "Exception within mod pack '${caller.simpleName}' thrown")
        appContextNullable?.sendHookExceptionEvent(e, message, null, isWarning, callerClass = caller)
        return null!!
    }
}
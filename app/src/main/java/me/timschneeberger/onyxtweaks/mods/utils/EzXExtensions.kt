package me.timschneeberger.onyxtweaks.mods.utils

import android.view.View
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.appContextNullable
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

typealias MethodParam = XC_MethodHook.MethodHookParam

/**
 * Finds a class by its qualified name, logging errors to the Xposed log.
 *
 * @throws ClassNotFoundException if the class cannot be found.
 */
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

/** Check if the class declares the method name. Superclasses are ignored. */
fun Class<*>.hasMethod(name: String) = declaredMethods.any { it.name == name }
fun Class<*>.hasMethods(vararg names: String): Boolean = names.all(::hasMethod)
/** Check if the class declares the method name. Superclasses are ignored. */
fun Class<*>.hasField(name: String) = declaredFields.any { it.name == name }
fun Class<*>.hasFields(vararg names: String): Boolean = names.all(::hasField)

fun Class<*>.requireCompanionObject(): Class<*> {
    return kotlin.companionObject?.java ?: run {
        throw IllegalStateException("Class '${this.name}' does not have a companion object")
    }
}
fun Class<*>.requireCompanionInstance(): Any {
    return kotlin.companionObjectInstance ?: run {
        throw IllegalStateException("Class '${this.name}' does not have a companion object")
    }
}

fun Class<*>.hasCompanion() = kotlin.companionObject != null

fun Class<*>.invokeCompanionMethod(name: String): Any? {
    return kotlin
        .companionObject
        ?.java
        ?.methodFinder()
        ?.firstByName(name)
        ?.invoke(kotlin.companionObjectInstance)
        ?: throw NullPointerException("Class '$name' does not have a companion object")
}

inline fun <T> T.applyObjectHelper(block: ObjectHelper.() -> Unit): T {
    this?.objectHelper(block)
    return this
}

/**
 * Filters the methods by name and returns the first one found.
 */
fun MethodFinder.firstByName(name: String): Method {
    return filterByName(name).first()
}

fun MethodParam.invokeOriginalMethodCatching(): Any? {
    return try {
        invokeOriginalMethod()
    }
    catch (e: Exception) {
        Log.ex("Error calling original method '${method.name}': ${e.message}")
        null
    }
}

fun MethodParam.invokeOriginalMethod(): Any? {
    try {
        return XposedBridge.invokeOriginalMethod(this.method, this.thisObject, this.args)
    }
    catch (e: InvocationTargetException) {
        // Unwrap the InvocationTargetException to get the actual exception
        throw e.targetException
    }
}

fun <TRet> MethodParam.runHookSafely(caller: KClass<*>, block: MethodParam.() -> TRet): TRet? {
    try {
        return block(this)
    }
    catch (e: Exception) {
        Log.ex(e, "Exception in hooked method '${method.name}' of class '${method.declaringClass.name}' within mod pack '${caller.simpleName}'")
        appContextNullable?.sendHookExceptionEvent(e, null, method, callerClass = caller)
        return null
    }
}

fun <T,TRet> T.runSafely(caller: KClass<*>, message: String, isWarning: Boolean = false, block: T.() -> TRet): TRet? {
    try {
        return block(this)
    }
    catch (e: Exception) {
        Log.ex(e, "Exception within mod pack '${caller.simpleName}' thrown; $message")
        appContextNullable?.sendHookExceptionEvent(e, message, null, isWarning, callerClass = caller)
        return null
    }
}

inline fun <reified T> View.findViewByIdName(name: String): T? = findViewByIdName(name) as T
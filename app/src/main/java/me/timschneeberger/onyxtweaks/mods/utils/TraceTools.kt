@file:Suppress("unused")
/**
 * This file contains utility functions for debugging and logging.
 */

package me.timschneeberger.onyxtweaks.mods.utils

import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.lang.reflect.Method

/**
 * Prints the method call details from the hook parameters to the Xposed log.
 */
fun MethodHookParam.printCall(tag: String = "TRACE") {
    val method = this.method as Method
    val argsString = StringBuilder("(").apply {
        args.forEach { arg ->
            if (arg == null)
                append("null,")
            else
                append(arg).append(",")
        }
        removeSuffix(",")
        append(")")
    }.toString()

    val clsName = method.declaringClass.simpleName
    val isVoid = method.returnType == Void.TYPE
    Log.ix("$tag [$clsName] ${method.name}$argsString => ${if (isVoid) "<void>" else result}")
}

/**
 * Traces all method calls in the specified class and prints each invocation
 * with parameters and return values to the Xposed log.
 */
fun Class<*>.traceClassCalls(
    tag: String = "TRACE",
    excludeMethods: List<String> = ArrayList()
) {
    MethodFinder.fromClass(this)
        .filter { excludeMethods.isEmpty() || !excludeMethods.contains(name) }
        .forEach { method ->
            method.createAfterHook { param ->
                param.printCall(tag)
            }
        }
}

/**
 * Recursively dumps the view hierarchy of the specified view and its children to the Xposed log.
 * Includes information about visibility, ID name, and class type of each view.
 */
fun View.dumpHierarchy() {
    fun dumpView(v: View, level: Int) {
        var name = runCatching { v.resources.getResourceName(v.id) }.getOrNull() ?: "**"
        val vis = when(v.visibility) {
            View.VISIBLE -> ""
            View.INVISIBLE -> "[HIDDEN] "
            View.GONE -> "[GONE] "
            else -> "[?]"
        }

        Log.ix("${"\t".repeat(level)}${vis} id $name type ${v.javaClass.getName()}")
    }

    fun dumpLevel(v: View, level: Int) {
        dumpView(v, level)
        if (v is ViewGroup) {
            (0..<v.childCount).forEach { i ->
                dumpLevel(v.getChildAt(i), level + 1)
            }
        }
    }

    dumpLevel(this, 0)
}
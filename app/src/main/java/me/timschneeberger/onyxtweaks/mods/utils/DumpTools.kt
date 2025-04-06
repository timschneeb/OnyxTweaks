@file:Suppress("unused")

package me.timschneeberger.onyxtweaks.mods.utils

import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.lang.reflect.Method

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

fun Class<*>.printClassCalls(
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

fun View.dumpIDs() {
    dumpIDs(this, 0)
}

private fun dumpIDs(v: View, level: Int) {
    dumpID(v, level)
    if (v is ViewGroup) {
        (0..<v.childCount).forEach { i ->
            dumpIDs(v.getChildAt(i), level + 1)
        }
    }
}

private fun dumpID(v: View, level: Int) {
    var name: String? = "**"

    runCatching {
        name = v.resources.getResourceName(v.id)
    }

    val vis = when(v.visibility) {
        View.VISIBLE -> ""
        View.INVISIBLE -> "[HIDDEN] "
        View.GONE -> "[GONE] "
        else -> "?"
    }

    Log.ix("\t".repeat(level) + vis + "id " + name + " type " + v.javaClass.getName())
}
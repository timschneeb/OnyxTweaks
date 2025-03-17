package me.timschneeberger.onyxtweaks.utils

import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.lang.reflect.Method

@Suppress("unused")
object DumpTools {
    fun printCall(param: MethodHookParam, tag: String) {
        val method = param.method as Method

        val args = StringBuilder("(").apply {
            param.args.forEach { arg ->
                if (arg == null)
                    append("null,")
                else
                    append(arg).append(",")
            }
            removeSuffix(",")
            append(")")
        }.toString()

        val clsName = method.declaringClass.simpleName;
        val isVoid = method.returnType == Void.TYPE
        Log.i("$tag [$clsName] ${method.name}$args => ${if (isVoid) "<void>" else param.result}")
    }

    fun printClassCalls(
        cls: Class<*>,
        tag: String,
        excludeMethods: List<String> = ArrayList()
    ) {
        MethodFinder.fromClass(cls)
            .filter { excludeMethods.isEmpty() || !excludeMethods.contains(name) }
            .forEach { method ->
                method.createAfterHook { param ->
                    printCall(param, tag)
                }
            }
    }

    fun dumpIDs(v: View) {
        dumpIDs(v, 0)
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

        runSafelySilent {
            name = v.resources.getResourceName(v.id)
        }

        Log.i("\t".repeat(level) + "id " + name + " type " + v.javaClass.getName())
    }
}
package me.timschneeberger.onyxtweaks.utils

import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.ClassFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
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
        XposedBridge.log("$tag [$clsName] ${method.name}$args => ${if (isVoid) "<void>" else param.result}")
    }

    fun logClassCalls(
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

    fun dumpClass(className: String) {
        val ourClass = ClassFinder.of(className)?.first() ?: return
        dumpClass(ourClass)
    }

    fun dumpClass(ourClass: Class<*>) {
        XposedBridge.log("Class: ${ourClass.name}")
        XposedBridge.log("extends: ${ourClass.superclass.name}")
        XposedBridge.log("Subclasses:")
        ourClass.declaredClasses.forEach { XposedBridge.log(it.name) }
        XposedBridge.log("Methods:")

        ourClass.declaredConstructors.forEach { m ->
            XposedBridge.log("${m.name} -  - ${m.parameterCount}")
            m.parameterTypes.forEach {
                XposedBridge.log("\t\t${it.typeName}")
            }
        }

        ourClass.declaredMethods.forEach { m ->
            XposedBridge.log("${m.name} - " + m.returnType + " - ${m.parameterCount}")
            m.parameterTypes.forEach {
                XposedBridge.log("\t\t${it.typeName}")
            }
        }

        XposedBridge.log("Fields:")
        ourClass.declaredFields.forEach { f ->
            XposedBridge.log("\t\t${f.name} - ${f.type.name}")
        }

        XposedBridge.log("")
    }

    fun dumpParentIDs(v: View) {
        dumpParentIDs(v, 0)
    }

    private fun dumpParentIDs(v: View, level: Int) {
        dumpID(v, level)
        runSafelySilent {
            if (v.parent is View) {
                dumpParentIDs((v.parent as View?)!!, level + 1)
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

        XposedBridge.log("\t".repeat(level) + "id " + name + " type " + v.javaClass.getName())
    }
}
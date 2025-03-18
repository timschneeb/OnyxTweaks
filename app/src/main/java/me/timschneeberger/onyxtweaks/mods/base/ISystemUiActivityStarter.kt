package me.timschneeberger.onyxtweaks.mods.base

import android.content.Intent
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import me.timschneeberger.onyxtweaks.mods.utils.firstByNameOrLog
import me.timschneeberger.onyxtweaks.mods.utils.getClass

interface ISystemUiActivityStarter {
    fun getActivityStarter(): Any? = MethodFinder.fromClass("com.android.systemui.Dependency")
            .filterByParamTypes(Class::class.java)
            .firstByNameOrLog("get")
            .invoke(null, getClass("com.android.systemui.plugins.ActivityStarter"))

    fun startActivityDismissingKeyguard(intent: Intent, flags: Int = 0) {
        getActivityStarter()?.let {
            it.javaClass
                .methodFinder()
                .filterByParamTypes(Intent::class.java, Int::class.javaPrimitiveType)
                .firstByNameOrLog("postStartActivityDismissingKeyguard")
                .invoke(it, intent, flags)
        } ?: Log.w("ActivityStarter not found")
    }
}
package me.timschneeberger.onyxtweaks.mods.base

import android.content.Intent
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.getClass

interface ISystemUiActivityStarter {
    fun getActivityStarter(): Any? = MethodFinder.fromClass("com.android.systemui.Dependency")
            .filterByParamTypes(Class::class.java)
            .firstByName("get")
            .invoke(null, getClass("com.android.systemui.plugins.ActivityStarter"))

    fun startActivityDismissingKeyguard(intent: Intent, flags: Int = 0) {
        with(getActivityStarter()) {
            javaClass
                .methodFinder()
                .filterByParamTypes(Intent::class.java, Int::class.javaPrimitiveType)
                .firstByName("postStartActivityDismissingKeyguard")
                .invoke(this, intent, flags)
        }
    }
}
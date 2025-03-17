package me.timschneeberger.onyxtweaks.mods

import android.app.Instrumentation
import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.IEarlyZygoteHook
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.runSafely
import kotlin.reflect.KClass

class ModManager {
    fun handleLoadPackage(lpParam: LoadPackageParam) {

        val onContextReady = fun (context: Context) {
            if (context.packageName != EzXHelper.hostPackageName)
                XposedBridge.log("Context package name does not match host package name! Context not updated")
            else
                EzXHelper.initAppContext(context, addPath = true)

            getPacksForPackage(lpParam.packageName)
                .forEach {
                    XposedBridge.log("Initializing mod pack: ${it::class.java.simpleName}")
                    runSafely { it.handleLoadPackage(lpParam) }
                }
        }

        if (lpParam.packageName == SYSTEM_FRAMEWORK_PACKAGE)
            MethodFinder.fromClass("com.android.server.policy.PhoneWindowManager")
                .filterNonAbstract()
                .filterByName("init")
                .forEach { constructor ->
                    constructor.createBeforeHook { param ->
                        param.args
                            .firstOrNull { it is Context }
                            ?.let { return@createBeforeHook onContextReady(it as Context) }

                        XposedBridge.log("No context found in PhoneWindowManager constructor!")
                    }
                }
        else {
            MethodFinder.fromClass(Instrumentation::class.java)
                .filterByName("newApplication")
                .forEach { constructor ->
                    constructor.createBeforeHook { param ->
                        param.args
                            .firstOrNull { it is Context }
                            ?.let { return@createBeforeHook onContextReady(it as Context) }

                        XposedBridge.log("No context found in newApplication!")
                    }
                }
        }
    }

    fun handleInitPackageResources(param: InitPackageResourcesParam) {
        runSafely {
            getPacksForPackage(param.packageName).forEach { it.handleInitPackageResources(param) }
        }
    }

    fun initZygote(param: IXposedHookZygoteInit.StartupParam) {
        synchronized(this) {
            ModPacks.available
                .filter { it.java.interfaces.contains(IEarlyZygoteHook::class.java) }
                .map(::ensurePackInitialized)
                .map { it as IEarlyZygoteHook }
                .forEach { mod ->
                    runSafely { mod.handleZygoteInit(param) }
                }
        }
    }

    private fun getPacksForPackage(packageName: String): List<ModPack> {
        synchronized(this) {
            runningMods
                .filter { it.targetPackages.contains(packageName) }
                .let {
                    if(it.isNotEmpty()) {
                        return it
                    }
                }

            return ModPacks.available
                .filter { ModPack.getTargetPackages(it).contains(packageName) }
                .map(::ensurePackInitialized)
        }
    }

    private fun ensurePackInitialized(cls: KClass<*>): ModPack {
        runningMods
            .firstOrNull { it::class == cls }
            ?.let { return it }

        return cls.java
            .getDeclaredConstructor()
            .newInstance()
            .run { this as ModPack }
            // Ensure preferences are initialized at this point
            .also { pack -> pack.preferences }
    }

    companion object {
        private var runningMods: MutableList<ModPack> = mutableListOf()
    }
}
package me.timschneeberger.onyxtweaks.mods

import android.app.Instrumentation
import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.mods.Constants.GLOBAL
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.IEarlyZygoteHook
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createBeforeHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.runSafely
import kotlin.reflect.KClass

class ModManager {
    fun initZygote(param: IXposedHookZygoteInit.StartupParam) {
        synchronized(this) {
            ModPacks.available
                .filter { it.java.interfaces.contains(IEarlyZygoteHook::class.java) }
                .map(::ensurePackInitialized)
                .map { it as IEarlyZygoteHook }
                .forEach { mod ->
                    param.runSafely(mod::handleZygoteInit)
                }
        }
    }

    fun handleLoadPackage(lpParam: LoadPackageParam) {
        runSafely {
            if (lpParam.packageName == SYSTEM_FRAMEWORK_PACKAGE)
                MethodFinder.fromClass("com.android.server.policy.PhoneWindowManager")
                    .filterNonAbstract()
                    .filterByName("init")
                    .forEach { constructor ->
                        constructor.createBeforeHookCatching hook@ { param ->
                            param.args
                                .firstOrNull { it is Context }
                                ?.let { return@hook onContextReady(it as Context, lpParam) }

                            Log.ex("No context found in PhoneWindowManager constructor!")
                        }
                    }
            else {
                MethodFinder.fromClass(Instrumentation::class.java)
                    .filterByName("newApplication")
                    .forEach { constructor ->
                        constructor.createBeforeHookCatching hook@ { param ->
                            param.args
                                .firstOrNull { it is Context }
                                ?.let { return@hook onContextReady(it as Context, lpParam) }

                            Log.ex("No context found in newApplication!")
                        }
                    }
            }
        }
    }

    fun handleInitPackageResources(param: InitPackageResourcesParam) {
        runSafely {
            getPacksForPackage(param.packageName).forEach { it.handleInitPackageResources(param) }
        }
    }

    private fun onContextReady(context: Context, param: LoadPackageParam) {
        if (context.packageName != EzXHelper.hostPackageName)
            Log.dx("Context package name does not match host package name! Context not updated. (${context.packageName} != ${EzXHelper.hostPackageName})")
        else
            EzXHelper.initAppContext(context, addPath = true)

        getPacksForPackage(param.packageName)
            .forEach { pack ->
                Log.dx("Initializing mod pack: ${pack::class.java.simpleName}")
                param.runSafely(pack::handleLoadPackage)
            }
    }

    private fun getPacksForPackage(packageName: String): List<ModPack> {
        synchronized(this) {
            runningMods
                .filter { it.targetPackages.contains(GLOBAL) || it.targetPackages.contains(packageName) }
                .let {
                    if(it.isNotEmpty()) {
                        return it
                    }
                }

            return ModPacks.available
                .filter { ModPack.getTargetPackages(it).let { pkgs -> pkgs.contains(GLOBAL) || pkgs.contains(packageName) } }
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
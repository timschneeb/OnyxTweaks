package me.timschneeberger.onyxtweaks.mods

import android.app.Instrumentation
import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.bridge.registerModEventReceiver
import me.timschneeberger.onyxtweaks.mods.Constants.GLOBAL
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.IEarlyZygoteHook
import me.timschneeberger.onyxtweaks.mods.base.IResourceHook
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.runSafely
import kotlin.reflect.KClass

class ModManager {
    fun initZygote(param: IXposedHookZygoteInit.StartupParam) {
        synchronized(this) {
            ModRegistry.modsWithZygoteHook
                .map(::ensurePackInitialized)
                .map { it as IEarlyZygoteHook }
                .forEach { mod ->
                    param.runSafely(
                        mod::class,
                        "Error while handling IEarlyZygoteHooks",
                        block = mod::handleZygoteInit
                    )
                }
        }
    }

    fun handleLoadPackage(lpParam: LoadPackageParam) {
        getPacksForPackage(lpParam.packageName)
            .forEach { mod ->
                lpParam.runSafely(mod::class, "Error while hooking DEX code (early hook)", block = mod::handleEarlyLoadPackage)
            }

        runSafely(ModManager::class, "Failed to hook application/framework entrypoint") {
            if (lpParam.packageName == SYSTEM_FRAMEWORK_PACKAGE)
                MethodFinder.fromClass("com.android.server.policy.PhoneWindowManager")
                    .filterNonAbstract()
                    .filterByName("init")
                    .forEach { constructor ->
                        constructor.createBeforeHook hook@ { param ->
                            runSafely(ModManager::class, "Failed to retrieve framework context and initialize mods") {
                                param.args
                                    .firstOrNull { it is Context }
                                    ?.let { return@runSafely onContextReady(it as Context, lpParam) }

                                throw IllegalStateException("No context found in PhoneWindowManager constructor!")
                            }
                        }
                    }
            else {
                MethodFinder.fromClass(Instrumentation::class.java)
                    .filterByName("newApplication")
                    .forEach { constructor ->
                        constructor.createBeforeHook hook@ { param ->
                            runSafely(ModManager::class, "Failed to retrieve app context and initialize mods") {
                                param.args
                                    .firstOrNull { it is Context }
                                    ?.let { return@runSafely onContextReady(it as Context, lpParam) }

                                throw IllegalStateException("No context found in newApplication!")
                            }
                        }
                    }
            }
        }
    }

    fun handleInitPackageResources(param: InitPackageResourcesParam) {
        if (ModRegistry.packagesWithResourceHooks.contains(param.packageName) ||
            ModRegistry.packagesWithResourceHooks.contains(GLOBAL)) {

            getPacksForPackage(param.packageName).forEach { mod ->
                if (mod is IResourceHook) {
                    param.runSafely(
                        mod::class,
                        "Error while hooking resources",
                        block = mod::handleInitPackageResources
                    )
                }
            }
        }
    }

    private fun onContextReady(context: Context, param: LoadPackageParam) {
        if (context.packageName != EzXHelper.hostPackageName) {
            Log.dx("Context package name does not match host package name! Context not updated. (${context.packageName} != ${EzXHelper.hostPackageName})")
            EzXHelper.addModuleAssetPath(context)
        }
        else
            EzXHelper.initAppContext(context, addPath = true)

        getPacksForPackage(param.packageName)
            .forEach { mod ->
                Log.dx("Initializing mod pack with context: ${mod::class.java.simpleName}")
                context.registerModEventReceiver(mod)
                param.runSafely(mod::class, "Error while hooking DEX code", block = mod::handleLoadPackage)
            }
    }

    private fun getPacksForPackage(packageName: String): List<ModPack> {
        synchronized(this) {
            return (ModRegistry.modsByPackage[packageName] ?: ModRegistry.modsByPackage[GLOBAL])
                ?.map(::ensurePackInitialized)
                ?: emptyList()
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
            .also { pack -> runningMods.add(pack) }
    }

    companion object {
        private var runningMods: MutableList<ModPack> = mutableListOf()
    }
}
package me.timschneeberger.onyxtweaks.mods

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
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
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.runSafely
import me.timschneeberger.onyxtweaks.utils.cast
import kotlin.reflect.KClass

/**
 * This class manages the loading of all mod packs.
 * It is responsible for calling the correct methods in the correct order.
 *
 * [initZygote], [handleLoadPackage], and [handleInitPackageResources] must be called
 * from the main Xposed hook entrypoint.
 */
class ModManager {
    /**
     * This method is called when the zygote is initialized.
     * It is responsible for initializing all mod packs that implement [IEarlyZygoteHook].
     */
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

    /**
     * This method is called when a package is loaded.
     * It is responsible for initializing all mod packs that for a specific package and attaching
     * application context hooks.
     *
     * It calls [me.timschneeberger.onyxtweaks.mods.base.ModPack.handleEarlyLoadPackage] for
     * relevant mod packs immediately, before receiving a context instance.
     * [me.timschneeberger.onyxtweaks.mods.base.ModPack.handleLoadPackage] is called when
     * the context has been obtained.
     */
    fun handleLoadPackage(lpParam: LoadPackageParam) {
        getPacksForPackage(lpParam.packageName)
            .forEach { mod ->
                lpParam.runSafely(mod::class, "Error while hooking DEX code (early hook)", block = mod::handleEarlyLoadPackage)
            }

        runSafely(ModManager::class, "Failed to hook application/framework entrypoint") {
            if (lpParam.packageName == SYSTEM_FRAMEWORK_PACKAGE)
                //
                MethodFinder.fromClass("com.android.server.policy.PhoneWindowManager")
                    .filterNonAbstract()
                    .filterByName("init")
                    .forEach { constructor ->
                        constructor.createBeforeHook hook@ { param ->
                            runSafely(ModManager::class, "Failed to retrieve framework context and initialize mods") {
                                param.args[0]
                                    .cast<Context>()
                                    ?.let { return@runSafely onContextReady(it, lpParam) }

                                throw IllegalStateException("No context found in PhoneWindowManager constructor!")
                            }
                        }
                    }
            else {
                // Hook that obtains the app context
                MethodFinder.fromClass(Instrumentation::class.java)
                    .filterByName("newApplication")
                    .forEach { constructor ->
                        constructor.createBeforeHook hook@ { param ->
                            runSafely(ModManager::class, "Failed to retrieve app context and initialize mods") {
                                param.args[2]
                                    .cast<Context>()
                                    ?.let { return@runSafely onContextReady(it, lpParam) }

                                throw IllegalStateException("No context found in newApplication!")
                            }
                        }
                    }

                // Also hook alternative context sources, so we can inject our module asset path
                MethodFinder.fromClass("android.app.ActivityThread")
                    .filterByName("performLaunchActivity")
                    .forEach { constructor ->
                        constructor.createAfterHook hook@ { param ->
                            param.result
                                .cast<Context>()
                                ?.let(this@ModManager::onAlternativeContextReady)
                                ?: Log.wx("Failed to retrieve app context from ActivityThread")
                        }
                    }

                MethodFinder.fromClass(ContextWrapper::class.java)
                    .filterByParamTypes(Context::class.java)
                    .firstByName("attachBaseContext")
                    .createAfterHook hook@ { param ->
                        param.args.first()
                            .cast<Context>()
                            ?.let(this@ModManager::onAlternativeContextReady)
                            ?: Log.wx("Failed to retrieve app context from ContextWrapper")
                    }
            }
        }
    }

    /**
     * This method is called when the resources of a package are initialized.
     * It is responsible for initializing all mod packs that implement [IResourceHook].
     */
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

    /**
     * Called when the main context of the app has been received.
     * This method must only be called once per package.
     */
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

    /**
     * Called when the context instances from other parts of the app are received.
     * This method can be called multiple times.
     *
     * This is needed to inject our module asset path into dynamically created contexts.
     */
    private fun onAlternativeContextReady(context: Context) {
        runSafely(ModManager::class, "Failed to inject module asset path into context instance $context") {
            EzXHelper.addModuleAssetPath(context)
        }
    }

    /**
     * Lazily loads the mod packs for the given package name.
     */
    private fun getPacksForPackage(packageName: String): List<ModPack> {
        synchronized(this) {
            return (ModRegistry.modsByPackage[packageName] ?: ModRegistry.modsByPackage[GLOBAL])
                ?.map(::ensurePackInitialized)
                ?: emptyList()
        }
    }

    /**
     * Lazily loads the mod pack by its class handle.
     */
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
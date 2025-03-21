package me.timschneeberger.onyxtweaks.mods.utils.unused

import android.content.ComponentName
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast

@TargetPackages(Constants.SYSTEM_FRAMEWORK_PACKAGE)
class EacPerActivityOverrides : ModPack() {
    override val group = PreferenceGroups.MISC

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        findClass("android.onyx.optimization.data.v2.EACAppConfig").apply {
            /*
             * Get the activity config map
             * This is the main source of activity configs
             * Important: Any direct field access (getter only) to activityConfigMap MUST be hooked!
             */
            methodFinder()
                .firstByName("getActivityConfigMap")
                .createAfterHookCatching hook@ { param ->
                    // Get current map or create a new one if null
                    val activityConfigMap = param.result
                        .cast<HashMap<String, Any>>()
                        ?: HashMap<String, Any>()

                    val pkgName= methodFinder()
                        .firstByName("getPkgName")
                        .invoke(param.thisObject)
                        ?.toString()

                    if (pkgName == null) {
                        Log.ex("EacPerActivityOverrides: pkgName is null")
                        return@hook
                    }

                    injectCustomConfig(pkgName, param.thisObject, activityConfigMap)
                    param.result = activityConfigMap
                }

            /*
             * Obtain activity config by class name
             * If null, returns the app-global activity config
             *
             * Hook: Prevents direct field access to activityConfigMap
             */
            methodFinder()
                .filterByParamTypes(String::class.java)
                .firstByName("obtainActivityConfig")
                .createAfterHookCatching hook@ { param ->
                    val clsName = param.args[0]
                    findActivityConfigByExactName(param.thisObject, clsName as? String)?.let { config ->
                        param.result = config
                    }

                   // printCall(param)
                }

            /*
             * Fuzzy matching is only used to get activity configs for:
             *      - activityConfig.getPaintConfig().isDitherBitmap()
             *      - getReadOnlyAppConfig().fuzzyMatchActivityConfig(...).getNoteConfig()
             *
             * Hook: Prevents direct field access to activityConfigMap
             */
            methodFinder()
                .filterByParamTypes(String::class.java)
                .firstByName("fuzzyMatchActivityConfig")
                .createAfterHookCatching hook@ { param ->
                   // printCall(param)
                    val clsName = param.args[0] as? String

                    // First check for exact match
                    findActivityConfigByExactName(param.thisObject, clsName)?.let { config ->
                        param.result = config
                        return@hook
                    }

                    // Then check for fuzzy match
                    getActivityMap(param.thisObject)
                        ?.entries
                        ?.firstOrNull { clsName != null && clsName.contains(it.key.toString()) }
                        ?.let {
                            Log.dx("fuzzyMatchActivityConfig: $clsName found via fuzzy match in activityConfigMap")
                            param.result = it.value
                        }
                }

            /*
             * Get display config for a specific activity
             *
             * Hook: Prevents direct field access to activityConfigMap
             */
            methodFinder()
                .filterByParamTypes(ComponentName::class.java)
                .firstByName("getDisplayConfig")
                .createAfterHookCatching hook@ { param ->
                   // printCall(param)

                    val component = param.args[0] as? ComponentName
                    if (component == null)
                    // App global config requested, ignore for now
                        return@hook

                    findActivityConfigByExactName(param.thisObject, component.className)?.let { config ->
                        Log.dx("getDisplayConfig: config for ${component.className} found")
                        param.result = config::class.java
                            .methodFinder()
                            .firstByName("getDisplayConfig")
                            .invoke(config)
                    }
                }

            /*
             * Get display config for a specific activity
             *
             * Hook: Prevents direct field access to activityConfigMap
             */
            methodFinder()
                .filterByParamTypes(ComponentName::class.java)
                .firstByName("getRefreshConfig")
                .createAfterHookCatching hook@ { param ->
                   // printCall(param)

                    val component = param.args[0] as? ComponentName
                    if (component == null)
                    // App global config requested, ignore for now
                        return@hook

                    findActivityConfigByExactName(param.thisObject, component.className)?.let { config ->
                        Log.dx("getRefreshConfig: config for ${component.className} found")
                        param.result = config::class.java
                            .methodFinder()
                            .firstByName("getRefreshConfig")
                            .invoke(config)
                    }
                }
        }
    }

    private fun injectCustomConfig(pkgName: String, appConfigInstance: Any, activityMap: HashMap<String, Any>) {
        val modConfig = getGlobalActivityConfig(appConfigInstance)
            ?.let { global ->

            EACActivityConfigWrapper(copy(global)).apply {
                refreshConfig.enable = true
                refreshConfig.turbo = 0
                refreshConfig.updateMode = 2
            }.unwrap()
        } ?: run {
            Log.ex("injectCustomConfig: Global activity config is null")
            return
        }
        val customConfig = mapOf(
            "eu.kanade.tachiyomi.ui.reader.ReaderActivity" to modConfig
        )

        customConfig.forEach { (clsName, config) ->
            activityMap[clsName] = config
            Log.dx("injectCustomConfig: Injected custom config for $clsName in $pkgName")
        }
    }

    private fun getActivityMap(appConfigInstance: Any): Map<*, *>? {
        return appConfigInstance::class.java
            .methodFinder()
            .firstByName("getActivityConfigMap")
            .invoke(appConfigInstance)
            .cast<Map<*,*>>()
    }

    private fun getGlobalActivityConfig(appConfigInstance: Any): Any? {
        return appConfigInstance::class.java
            .methodFinder()
            .firstByName("getGlobalActivityConfig")
            .invoke(appConfigInstance)
    }

    private fun findActivityConfigByExactName(appConfigInstance: Any, clsName: String?): Any? {
        if (clsName == null) {
            Log.ex("findActivityConfigByExactName: clsName parameter is null")
            return null
        }

        return getActivityMap(appConfigInstance)?.let { map -> map[clsName] }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> copy(entity: T): T  {
        var clazz = entity::class.java as? Class<*>
        val newEntity = clazz?.getDeclaredConstructor()?.newInstance()
        while (clazz != null) {
            copyFields(entity, newEntity, clazz)
            clazz = clazz.superclass
        }
        return newEntity as T
    }

    private fun <T> copyFields(entity: T, newEntity: T, clazz: Class<*>): T {
        val fields = clazz.declaredFields
        for (field in fields) {
            field.isAccessible = true

            // Deep copy the nested classes that we are going to modify
            if(field.type.name.contains("RefreshConfig")) {
                field.set(newEntity, copy(field.get(entity)))
            }
            else
                field.set(newEntity, field.get(entity))
        }
        return newEntity
    }
}
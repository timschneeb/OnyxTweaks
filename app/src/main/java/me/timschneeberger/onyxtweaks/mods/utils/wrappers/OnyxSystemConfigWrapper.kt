package me.timschneeberger.onyxtweaks.mods.utils.wrappers

import me.timschneeberger.onyxtweaks.mods.utils.findClass

class OnyxSystemConfigWrapper(
    item: Any
) : ObjectWrapper(item) {

    constructor() : this(
        findClass("android.onyx.config.OnyxSystemConfig")
            .getDeclaredConstructor()
            .newInstance()
    )


    fun getConfigMap(): Map<String, Set<String>> {
        return get("configMap") ?: emptyMap()
    }
}
package me.timschneeberger.onyxtweaks.mods.utils.unused

import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.wrappers.ObjectWrapper

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
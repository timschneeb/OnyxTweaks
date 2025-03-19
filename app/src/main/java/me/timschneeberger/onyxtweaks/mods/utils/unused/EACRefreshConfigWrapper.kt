package me.timschneeberger.onyxtweaks.mods.utils.unused

import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.wrappers.ObjectWrapper

// Source type: android.onyx.optimization.data.v2.EACRefreshConfig (framework.jar)
class EACRefreshConfigWrapper(item: Any) : ObjectWrapper(item) {
    /*
       Available members:
            int animationDuration;
            String animationType;
            boolean supportRegal;
            int updateMode;
            boolean useGCForNewSurface;
            int gcInterval = 20;
            int turbo = 0;
            int antiFlicker = EACConfig.singleton().getAntiFlickerDefault();

        Update mode:
            0 = Normal
            1 = Speed
            2 = A2
            3 = Regal
            4 = X

         Turbo: 1-6 (only in A2 mode)
     */

    constructor() : this(
        findClass("android.onyx.optimization.data.v2.EACRefreshConfig")
            .getDeclaredConstructor()
            .newInstance()
    )

    // From superclass
    var enable: Boolean
        get() = get("enable") ?: false
        set(value) = set("enable", value)

    var animationDuration: Int
        get() = get("animationDuration") ?: 0
        set(value) = set("animationDuration", value)

    var animationType: String
        get() = get("animationType") ?: ""
        set(value) = set("animationType", value)

    var updateMode: Int
        get() = get("updateMode") ?: 0
        set(value) = set("updateMode", value)

    var useGCForNewSurface: Boolean
        get() = get("useGCForNewSurface") ?: false
        set(value) = set("useGCForNewSurface", value)

    var gcInterval: Int
        get() = get("gcInterval") ?: 20
        set(value) = set("gcInterval", value)

    var turbo: Int
        get() = get("turbo") ?: 0
        set(value) = set("turbo", value)

    var antiFlicker: Int
        get() = get("antiFlicker") ?: 0
        set(value) = set("antiFlicker", value)
}
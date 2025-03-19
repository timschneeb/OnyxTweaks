package me.timschneeberger.onyxtweaks.mods.utils.unused

import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.wrappers.ObjectWrapper

// Source type: android.onyx.optimization.data.v2.EACDisplayConfig (framework.jar)
class EACDisplayConfigWrapper(item: Any) : ObjectWrapper(item) {
    /*
      Available members:
            int contrast = EACConfig.singleton().getContrastDefault();
            int monoLevel = 10;
            int cfaColorSaturation = EACConfig.singleton().getCfaSaturationDefault();
            int cfaColorSaturationMin = EACConfig.singleton().getSaturationMinValue();
            int cfaColorBrightness = 0;
            int ditherThreshold = EACConfig.singleton().getDitherThreshold();
            int bwMode = 0;
            boolean enhance = true;
     */

    constructor() : this(
        findClass("android.onyx.optimization.data.v2.EACDisplayConfig")
            .getDeclaredConstructor()
            .newInstance()
    )

    // From superclass
    var enable: Boolean
        get() = get("enable") ?: false
        set(value) = set("enable", value)

    var contrast: Int
        get() = get("contrast") ?: 0
        set(value) = set("contrast", value)

    var monoLevel: Int
        get() = get("monoLevel") ?: 10
        set(value) = set("monoLevel", value)

    var cfaColorSaturation: Int
        get() = get("cfaColorSaturation") ?: 0
        set(value) = set("cfaColorSaturation", value)

    var cfaColorSaturationMin: Int
        get() = get("cfaColorSaturationMin") ?: 0
        set(value) = set("cfaColorSaturationMin", value)

    var cfaColorBrightness: Int
        get() = get("cfaColorBrightness") ?: 0
        set(value) = set("cfaColorBrightness", value)

    var ditherThreshold: Int
        get() = get("ditherThreshold") ?: 0
        set(value) = set("ditherThreshold", value)

    var bwMode: Int
        get() = get("bwMode") ?: 0
        set(value) = set("bwMode", value)

    var enhance: Boolean
        get() = get("enhance") ?: true
        set(value) = set("enhance", value)
}
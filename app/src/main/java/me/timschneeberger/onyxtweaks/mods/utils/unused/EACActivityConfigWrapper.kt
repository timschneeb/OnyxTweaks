package me.timschneeberger.onyxtweaks.mods.utils.unused

import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.wrappers.ObjectWrapper

// Source type: android.onyx.optimization.data.v2.EACActivityConfig (framework.jar)
class EACActivityConfigWrapper(item: Any) : ObjectWrapper(item) {
    /*
     * Available members:
     *      boolean isDisableScrollAnim;
     *      String clsName = "";
     *      EACDisplayConfig displayConfig = new EACDisplayConfig();
     *      EACRefreshConfig refreshConfig = new EACRefreshConfig();
     *      EACPaintConfig paintConfig = new EACPaintConfig();
     *      EACNoteConfig noteConfig = new EACNoteConfig();
     */
    constructor() : this(
        findClass("android.onyx.optimization.data.v2.EACActivityConfig")
            .getDeclaredConstructor()
            .newInstance()
    )

    var clsName: String
        get() = get("clsName") ?: ""
        set(value) = set("clsName", value)

    var displayConfig: EACDisplayConfigWrapper
        get() = EACDisplayConfigWrapper(get("displayConfig")!!)
        set(value) = set("displayConfig", value.unwrap())

    var refreshConfig: EACRefreshConfigWrapper
        get() = EACRefreshConfigWrapper(get("refreshConfig")!!)
        set(value) = set("refreshConfig", value.unwrap())
}
package me.timschneeberger.onyxtweaks.mods.wrappers

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import me.timschneeberger.onyxtweaks.mods.utils.firstByName

// Source type: com.onyx.common.applications.model.AppItemModel (com.onyx)
class AppItemModelWrapper(item: Any) : ObjectWrapper(item) {
    var page: Int?
        get() = get("page")
        set(value) = set("page", value)

    var x: Int
        get() = get("x") ?: -1
        set(value) = set("x", value)

    var y: Int
        get() = get("y") ?: -1
        set(value) = set("y", value)

    var id: Long
        get() = get("id") ?: -1
        set(value) = set("id", value)

    fun save() = item::class.java
        .methodFinder()
        .filterByParamCount(0)
        .firstByName("save")
        .invoke(item)
}
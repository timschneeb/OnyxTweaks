package me.timschneeberger.onyxtweaks.wrappers

import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper

open class ObjectWrapper(protected val item: Any) {
    fun set(name: String, value: Any?) =
        item.objectHelper().setObjectUntilSuperclass(name, value)

    fun <T> get(name: String): T? = item
        .objectHelper()
        .getObjectOrNullUntilSuperclassAs<T>(name)

    override fun toString(): String = item.toString()

    fun unwrap(): Any = item
}
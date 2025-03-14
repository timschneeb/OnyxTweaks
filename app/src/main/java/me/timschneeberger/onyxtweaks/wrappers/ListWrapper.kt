package me.timschneeberger.onyxtweaks.wrappers

import me.timschneeberger.onyxtweaks.utils.castNonNull
import kotlin.reflect.KClass

class ListWrapper<T> where T : ObjectWrapper {
    val items: MutableList<T>

    constructor(input: Any, itemWrapperType: KClass<T>) {
        items = input
            .castNonNull<List<*>>()
            .filterNotNull()
            .map { itemWrapperType.java.getConstructor(Any::class.java).newInstance(it) }
            .toMutableList()
    }

    fun unwrap(): List<*> = items.map(ObjectWrapper::unwrap)
}
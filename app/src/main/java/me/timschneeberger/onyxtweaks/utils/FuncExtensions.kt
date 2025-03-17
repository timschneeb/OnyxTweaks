package me.timschneeberger.onyxtweaks.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.typeOf

@OptIn(ExperimentalContracts::class)
inline fun <reified T> Any?.cast(): T? {
    contract {
        returns() implies (this@cast is T?)
    }

    return when {
        this is T -> this
        this == null -> null
        else -> throw ClassCastException("Cannot cast $this to ${typeOf<T>()}")
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T> Any?.castNonNull(): T {
    contract {
        returns() implies (this@castNonNull is T)
    }

    return cast<T>()!!
}
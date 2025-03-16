package me.timschneeberger.onyxtweaks.utils

import com.topjohnwu.superuser.Shell
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.typeOf

fun restartPackageSilently(pkgName: String) {
    Shell.cmd(String.format("killall %s", pkgName)).exec()
}

fun restartZygote() {
    Shell.cmd("kill $(pidof zygote)").submit()
    Shell.cmd("kill $(pidof zygote64)").submit()
}

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
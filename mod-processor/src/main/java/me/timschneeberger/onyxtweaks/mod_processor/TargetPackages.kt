package me.timschneeberger.onyxtweaks.mod_processor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TargetPackages(vararg val targets: String)
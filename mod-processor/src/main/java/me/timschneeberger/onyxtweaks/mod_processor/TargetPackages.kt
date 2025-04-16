package me.timschneeberger.onyxtweaks.mod_processor

@Target(AnnotationTarget.CLASS)
annotation class TargetPackages(vararg val targets: String)
package me.timschneeberger.onyxtweaks.bridge

enum class ModEvents {
    HOOK_LOADED,
    LAUNCHER_REINITIALIZED,
    PREFERENCE_CHANGED,
    REQUEST_RESTART;

    // Event argument keys
    companion object {
        // HOOK_LOADED & REQUEST_RESTART
        const val ARG_PACKAGE = "package"
        // PREFERENCE_CHANGED
        const val ARG_PREF_GROUP = "preferenceGroup"
        const val ARG_PREF_KEY = "preferenceKey"
    }
}
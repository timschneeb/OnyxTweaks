package me.timschneeberger.onyxtweaks.bridge

enum class ModEvents {
    HOOK_LOADED,
    LAUNCHER_REINITIALIZED,
    SET_PREFERENCE,
    PREFERENCE_CHANGED,
    REQUEST_RESTART,
    HOOK_WARNING,
    HOOK_EXCEPTION;

    // Event argument keys
    companion object {
        // HOOK_LOADED & REQUEST_RESTART
        const val ARG_PACKAGE = "package"
        // PREFERENCE_CHANGED & SET_PREFERENCE
        const val ARG_PREF_GROUP = "preferenceGroup"
        const val ARG_PREF_KEY = "preferenceKey"
        const val ARG_PREF_TYPE = "preferenceType"
        // SET_PREFERENCE
        const val ARG_PREF_VALUE = "preferenceValue"
        // HOOK_WARNING & HOOK_EXCEPTION
        const val ARG_HOOKED_PACKAGE = "hookedPackage"
        const val ARG_HOOKED_METHOD = "hookedMethod"
        const val ARG_HOOKED_CLASS = "hookedClass"
        const val ARG_MESSAGE = "message"
        const val ARG_EXCEPTION = "exception"
    }
}
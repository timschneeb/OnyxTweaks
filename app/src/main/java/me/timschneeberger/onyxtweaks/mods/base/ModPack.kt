package me.timschneeberger.onyxtweaks.mods.base

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.moduleRes
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver.Companion.createEventIntent
import me.timschneeberger.onyxtweaks.bridge.ModEvents
import me.timschneeberger.onyxtweaks.bridge.OnModEventReceived
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.XPreferences

/**
 * Base class for all mod packs.
 */
abstract class ModPack : OnModEventReceived {
    /**
     * The preference group this mod pack belongs to.
     * This is used to identify the preferences that are accessed by this mod pack.
     */
    protected abstract val group: PreferenceGroups

    val preferences by lazy { XPreferences(group) }
    final override var modEventReceiver: ModEventReceiver? = null

    /**
     * Send a broadcast event to other mod packs or the Xposed module app.
     *
     * @param event the event to send
     * @param extras optional extras to include in the intent
     * @param callback optional callback to be called when the broadcast has been handled by
     *                 all registered receivers, if any
     */
    protected fun sendEvent(event: ModEvents, extras: Bundle? = null, callback: ((Intent) -> Unit)? = null) {
        if(callback == null)
            appContext.sendBroadcast(createEventIntent(event, this, extras))
        else {
            appContext.sendOrderedBroadcast(
                createEventIntent(event, this, extras),
                null,
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        callback.invoke(intent)
                    }
                },
                null,
                Activity.RESULT_OK,
                null,
                null
            )
        }
    }

    /**
     * Set a preference value for the preference group this module is using.
     *
     * @param keyRes the resource ID of the preference key
     * @param value the value to set
     * @param callback optional callback to be called when the broadcast has been handled by
     *                 all registered receivers, if any
     */
    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified T> setPreference(@StringRes keyRes: Int, value: Any?, noinline callback: ((Intent) -> Unit)? = null) {
        sendEvent(
            ModEvents.SET_PREFERENCE,
            Bundle().apply {
                putString(ModEvents.ARG_PREF_GROUP, group.name)
                putString(ModEvents.ARG_PREF_KEY, moduleRes.getString(keyRes))
                putString(ModEvents.ARG_PREF_TYPE, T::class.java.canonicalName)

                if (value != null) {
                    ModEvents.ARG_PREF_VALUE.let { key ->
                        when (T::class) {
                            Boolean::class -> putBoolean(key, value as Boolean)
                            String::class -> putString(key, value as String)
                            Int::class -> putInt(key, value as Int)
                            Long::class -> putLong(key, value as Long)
                            Float::class -> putFloat(key, value as Float)
                            Set::class -> putStringArray(key, (value as Set<String>).toTypedArray())
                            else -> throw IllegalArgumentException("Unknown type ${T::class}")
                        }
                    }
                }
            },
            callback
        )
    }

    final override fun onPreferenceGroupChanged(group: PreferenceGroups, key: String?) {
        if (group == this.group) {
            onPreferencesChanged(key)
            key?.let { Log.dx("Preference changed: $it") }
        }
    }

    /**
     * Called when a preference is changed.
     *
     * @param key the key of the preference that was changed
     *            or null if all or multiple preferences were changed
     */
    protected open fun onPreferencesChanged(key: String?) {}

    /**
     * Handle the loading of a package DEX code.
     * When this method is called, EzXHelper.appContext is available
     *
     * @param lpParam load package parameters
     */
    open fun handleLoadPackage(lpParam: LoadPackageParam) {}

    /**
     * Handle the loading of a package DEX code before the app context is initialized
     * When this method is called, EzXHelper.appContext is not available
     *
     * @param lpParam load package parameters
     */
    open fun handleEarlyLoadPackage(lpParam: LoadPackageParam) {}
}
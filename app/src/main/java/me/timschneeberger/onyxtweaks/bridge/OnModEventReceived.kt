package me.timschneeberger.onyxtweaks.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver.Companion.ACTION
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import kotlin.reflect.KClass

/**
 * Interface for receiving [ModEvents].
 *
 * Your class must declare the [modEventReceiver] property, which may be initialized with null.
 * [registerModEventReceiver] will initialize the property with a new instance of [ModEventReceiver].
 *
 * Use the context extension methods [registerModEventReceiver] and [unregisterModEventReceiver] to register
 * and unregister the receiver.
 */
interface OnModEventReceived {
    /**
     * The [ModEventReceiver] instance used to receive mod events.
     * This property will be initialized by [registerModEventReceiver] and can be set to null.
     */
    var modEventReceiver: ModEventReceiver?

    fun onModEventReceived(event: ModEvents, sender: String?, args: Bundle?) {}
    fun onHookLoaded(packageName: String) {}
    fun onPreferenceGroupChanged(group: PreferenceGroups, key: String?) {}
    fun onRestartRequested(packageName: String) {}
    fun onLauncherReinitialized() {}
    fun onHookException(
        exception: Throwable?,
        message: String?,
        isWarning: Boolean,
        packageName: String?,
        hookedMethod: String?,
        hookedClass: String?,
    ) {}
    fun onSetPreferenceRequested(
        group: PreferenceGroups,
        key: String,
        type: KClass<*>,
        value: Any?,
    ) {}
}

/**
 * Register a [OnModEventReceived] receiver to receive mod events.
 */
fun Context.registerModEventReceiver(receiver: OnModEventReceived) {
    receiver.modEventReceiver = ModEventReceiver().apply {
        addEventListener(receiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(this, IntentFilter(ACTION), Context.RECEIVER_EXPORTED)
        }
        else {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            registerReceiver(this, IntentFilter(ACTION))
        }
    }
}

/**
 * Unregister a [OnModEventReceived] receiver.
 */
fun Context.unregisterModEventReceiver(receiver: OnModEventReceived) {
    receiver.modEventReceiver?.let { unregisterReceiver(it) }
}
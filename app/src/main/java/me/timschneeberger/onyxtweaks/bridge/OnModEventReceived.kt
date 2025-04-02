package me.timschneeberger.onyxtweaks.bridge

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver.Companion.ACTION
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

interface OnModEventReceived {
    var modEventReceiver: ModEventReceiver?

    fun onModEventReceived(event: ModEvents, sender: String?, args: Bundle?) {}
    fun onHookLoaded(packageName: String) {}
    fun onPreferenceGroupChanged(group: PreferenceGroups, key: String?) {}
    fun onRestartRequested(packageName: String) {}
}

fun Context.registerModEventReceiver(receiver: OnModEventReceived) {
    receiver.modEventReceiver = ModEventReceiver().apply {
        addEventListener(receiver)
        registerReceiver(this, IntentFilter(ACTION), Context.RECEIVER_EXPORTED)
    }
}

fun Context.unregisterModEventReceiver(receiver: OnModEventReceived) {
    receiver.modEventReceiver?.let { unregisterReceiver(it) }
}
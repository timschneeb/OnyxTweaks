package me.timschneeberger.onyxtweaks.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.os.Build
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

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerModEventReceiver(receiver: OnModEventReceived) {
    receiver.modEventReceiver = ModEventReceiver().apply {
        addEventListener(receiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(this, IntentFilter(ACTION), Context.RECEIVER_EXPORTED)
        }
        else {
            registerReceiver(this, IntentFilter(ACTION))
        }
    }
}

fun Context.unregisterModEventReceiver(receiver: OnModEventReceived) {
    receiver.modEventReceiver?.let { unregisterReceiver(it) }
}
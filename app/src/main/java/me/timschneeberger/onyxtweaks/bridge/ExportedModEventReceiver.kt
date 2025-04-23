package me.timschneeberger.onyxtweaks.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences
import kotlin.reflect.KClass

/**
 * This receiver is registered in the manifest and can process intents
 * even when the app is stopped. This is important for events like LAUNCHER_INITIALIZED
 */
class ExportedModEventReceiver : BroadcastReceiver(), OnModEventReceived {
    override var modEventReceiver: ModEventReceiver? = null
        
    private var currentContext: Context? = null

    override fun onReceive(context: Context, intent: Intent) {
        currentContext = context

        if (modEventReceiver == null) {
            modEventReceiver = ModEventReceiver().apply {
                addEventListener(this@ExportedModEventReceiver)
            }
        }

        modEventReceiver?.onReceive(context, intent)
        currentContext = null
    }

    override fun onModEventReceived(event: ModEvents, sender: String?, args: Bundle?) {
        Log.d("Event received: $event from $sender")
    }

    override fun onLauncherReinitialized() {
        Preferences(currentContext!!, PreferenceGroups.LAUNCHER).apply {
            set(R.string.key_launcher_reinit_flag, false)
        }
        currentContext!!.toast(R.string.launcher_desktop_reinit_done_toast)
    }

    override fun onSetPreferenceRequested(
        group: PreferenceGroups,
        key: String,
        type: KClass<*>,
        value: Any?
    ) {
        Preferences(currentContext!!, group).apply { set(key, value, type) }
    }
}
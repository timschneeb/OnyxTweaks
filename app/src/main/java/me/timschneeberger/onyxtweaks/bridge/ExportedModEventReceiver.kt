package me.timschneeberger.onyxtweaks.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver.Companion.parseIntent
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences

/**
 * This receiver is registered in the manifest and can process intents
 * even when the app is stopped. This is important for events like LAUNCHER_INITIALIZED
 */
class ExportedModEventReceiver : BroadcastReceiver() {
    // TODO doesn't seem to work reliably
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("ExportedModEventReceiver received intent: ${intent.action}")

        parseIntent(intent)?.let { (event, sender, args) ->
            Log.i("Event received: $event from $sender")

            when (event) {
                ModEvents.LAUNCHER_REINITIALIZED -> {
                    Preferences(context, PreferenceGroups.LAUNCHER).apply {
                        set(R.string.key_launcher_reinit_flag, false)
                    }
                    context.toast(R.string.launcher_desktop_reinit_done_toast)
                }
                ModEvents.HOOK_LOADED -> {
                    Log.i("Hook loaded for package: ${args?.getString(ModEvents.ARG_PACKAGE)}")
                }
                else -> {}
            }
        }
    }
}
package me.timschneeberger.onyxtweaks.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.utils.CompatExtensions.getSerializableAs
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences

enum class ModEvents {
    LAUNCHER_REINITIALIZED
}

class ModEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) {
            Log.e("Received unknown action: ${intent.action}")
            return
        }

        intent.extras?.let {
            val event = it.getSerializableAs<ModEvents>(EXTRA_EVENT)
            val args = it.getBundle(EXTRA_ARGS)
            if (event == null) {
                Log.e("Received event is null")
                return@let
            }

            onEventReceive(context, event, args)
        }
    }

    private fun onEventReceive(context: Context, event: ModEvents, args: Bundle?) {
        Log.i("Event received: $event")

        when (event) {
            ModEvents.LAUNCHER_REINITIALIZED -> {
                Preferences(context, PreferenceGroups.LAUNCHER).apply {
                    set(R.string.key_launcher_reinit_flag, false)
                }
                context.toast(R.string.launcher_desktop_reinit_done_toast)
            }
        }
    }

    companion object {
        const val ACTION = "${BuildConfig.APPLICATION_ID}.action.MOD_EVENT"
        const val EXTRA_EVENT = "${BuildConfig.APPLICATION_ID}.extra.EVENT"
        const val EXTRA_ARGS = "${BuildConfig.APPLICATION_ID}.extra.ARGS"

        fun createIntent(event: ModEvents, args: Bundle? = null) =
            Intent(ACTION).apply {
                component = ComponentName(BuildConfig.APPLICATION_ID, ModEventReceiver::class.java.name)
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                putExtra(EXTRA_EVENT, event)
                args?.let { putExtra(EXTRA_ARGS, it) }
            }
    }
}
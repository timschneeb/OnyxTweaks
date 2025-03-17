package me.timschneeberger.onyxtweaks.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.base.LifecycleBroadcastHook
import me.timschneeberger.onyxtweaks.ui.utils.CompatExtensions.getSerializableAs
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences

enum class ModEvents {
    HOOK_LOADED,
    LAUNCHER_REINITIALIZED
}

internal interface OnModEventReceived {
    var modEventReceiver: BroadcastReceiver?

    fun onModEventReceived(event: ModEvents, args: Bundle?) {}
    fun onHookLoaded(packageName: String) {}

    fun Context.registerModEventReceiver() {
        modEventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                ModEventReceiver.parseIntent(intent)?.let { (event, args) ->
                    onModEventReceived(event, args)
                    when (event) {
                        ModEvents.HOOK_LOADED -> {
                            args?.getString(LifecycleBroadcastHook.ARG_PACKAGE)?.let(::onHookLoaded)
                        }
                        else -> {}
                    }
                }
            }
        }
        registerLocalReceiver(modEventReceiver!!, IntentFilter(ModEventReceiver.ACTION))
    }

    fun Context.unregisterModEventReceiver() {
        modEventReceiver?.let { unregisterLocalReceiver(it) }
    }
}

class ModEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        parseIntent(intent)?.let { (event, args) ->
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
            ModEvents.HOOK_LOADED -> {
                Log.i("Hook loaded for package: ${args?.getString(LifecycleBroadcastHook.ARG_PACKAGE)}")
            }
        }

        /* Send event to all local receivers
         * Note that the activity and other components may not be running when the event is received.
         * If the event is important, it should be handled directly in this receiver. */
        context.forwardModEvent(event, args)
    }

    companion object {
        const val ACTION = "${BuildConfig.APPLICATION_ID}.action.MOD_EVENT"
        const val EXTRA_EVENT = "${BuildConfig.APPLICATION_ID}.extra.EVENT"
        const val EXTRA_ARGS = "${BuildConfig.APPLICATION_ID}.extra.ARGS"

        fun parseIntent(intent: Intent): Pair<ModEvents, Bundle?>? {
            if (intent.action != ACTION) {
                Log.e("Received unknown action: ${intent.action}")
                return null
            }

            intent.extras?.let {
                val event = it.getSerializableAs<ModEvents>(EXTRA_EVENT)
                val args = it.getBundle(EXTRA_ARGS)
                if (event == null) {
                    Log.e("Received event is null")
                    return@let
                }

                return Pair(event, args)
            }
            return null
        }

        fun createIntent(event: ModEvents, args: Bundle? = null) =
            Intent(ACTION).apply {
                component = ComponentName(BuildConfig.APPLICATION_ID, ModEventReceiver::class.java.name)
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                putExtra(EXTRA_EVENT, event)
                args?.let { putExtra(EXTRA_ARGS, it) }
            }

        fun Context.forwardModEvent(event: ModEvents, args: Bundle? = null) {
            sendLocalBroadcast(createIntent(event, args))
        }
    }
}
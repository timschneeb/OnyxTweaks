package me.timschneeberger.onyxtweaks.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.ui.utils.CompatExtensions.getSerializableAs
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import kotlin.reflect.KClass

class ModEventReceiver : BroadcastReceiver() {
    private val eventListener = mutableListOf<OnModEventReceived>()

    override fun onReceive(context: Context, intent: Intent) {
        parseIntent(intent)?.let { (event, sender, args) ->
            eventListener.forEach { it.onModEventReceived(event, sender, args) }

            when (event) {
                ModEvents.HOOK_LOADED -> {
                    args?.getString(ModEvents.ARG_PACKAGE)?.let { pkg ->
                        eventListener.forEach { it.onHookLoaded(pkg) }
                    }
                }

                ModEvents.PREFERENCE_CHANGED -> {
                    val group = args?.getString(ModEvents.ARG_PREF_GROUP)
                    val key = args?.getString(ModEvents.ARG_PREF_KEY)
                    group ?: return@let null
                    eventListener.forEach {
                        it.onPreferenceGroupChanged(
                            PreferenceGroups.valueOf(group),
                            key
                        )
                    }
                }

                ModEvents.REQUEST_RESTART -> {
                    args?.getString(ModEvents.ARG_PACKAGE)?.let { pkg ->
                        eventListener.forEach { it.onRestartRequested(pkg) }
                    }
                }

                else -> {}
            }

        }
    }

    fun addEventListener(listener: OnModEventReceived) {
        eventListener.add(listener)
    }

    fun removeEventListener(listener: OnModEventReceived) {
        eventListener.remove(listener)
    }

    companion object {
        const val ACTION = "${BuildConfig.APPLICATION_ID}.action.MOD_EVENT"
        const val EXTRA_EVENT = "${BuildConfig.APPLICATION_ID}.extra.EVENT"
        const val EXTRA_SENDER = "${BuildConfig.APPLICATION_ID}.extra.SENDER"
        const val EXTRA_ARGS = "${BuildConfig.APPLICATION_ID}.extra.ARGS"

        fun parseIntent(intent: Intent): Triple<ModEvents, String?, Bundle?>? {
            if (intent.action != ACTION) {
                Log.e("Received unknown action: ${intent.action}")
                return null
            }

            intent.extras?.let {
                val event = it.getSerializableAs<ModEvents>(EXTRA_EVENT)
                val sender = it.getString(EXTRA_SENDER)
                val args = it.getBundle(EXTRA_ARGS)
                if (event == null) {
                    Log.e("Received event is null")
                    return@let
                }

                return Triple(event, sender, args)
            }
            return null
        }

        fun ModEvents.createIntent(sender: KClass<*>, args: Bundle? = null) =
            Intent(ACTION).apply {
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                putExtra(EXTRA_EVENT, this@createIntent)
                putExtra(EXTRA_SENDER, sender.qualifiedName)
                args?.let { putExtra(EXTRA_ARGS, it) }
            }

        // We inline and reify this function to able to obtain the calling class automatically
        inline fun <reified T : Any> T.createEventIntent(event: ModEvents, args: Bundle? = null) =
            event.createIntent(T::class, args)

        fun Context.sendEvent(event: ModEvents, sender: KClass<*>, args: Bundle? = null) {
            sendBroadcast(event.createIntent(sender::class, args))
        }

        inline fun <reified T> Context.sendEvent(event: ModEvents, args: Bundle? = null) {
            sendEvent(event, T::class, args)
        }
    }
}
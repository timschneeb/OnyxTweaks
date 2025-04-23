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

/**
 * BroadcastReceiver that receives and unpacks mod events.
 */
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
                    group ?: return@let
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

                ModEvents.LAUNCHER_REINITIALIZED -> {
                    eventListener.forEach { it.onLauncherReinitialized() }
                }

                ModEvents.HOOK_EXCEPTION, ModEvents.HOOK_WARNING -> {
                    args ?: return@let

                    val exception = args.getSerializableAs<Throwable>(ModEvents.ARG_EXCEPTION)
                    val message = args.getString(ModEvents.ARG_MESSAGE)
                    val packageName = args.getString(ModEvents.ARG_HOOKED_PACKAGE)
                    val hookedMethod = args.getString(ModEvents.ARG_HOOKED_METHOD)
                    val hookedClass = args.getString(ModEvents.ARG_HOOKED_CLASS)

                    eventListener.forEach {
                        it.onHookException(
                            exception,
                            message,
                            event == ModEvents.HOOK_WARNING,
                            packageName,
                            hookedMethod,
                            hookedClass
                        )
                    }
                }

                ModEvents.SET_PREFERENCE -> {
                    val groupName = args?.getString(ModEvents.ARG_PREF_GROUP)
                    val key = args?.getString(ModEvents.ARG_PREF_KEY)
                    val type = args?.getString(ModEvents.ARG_PREF_TYPE)

                    if (groupName == null || key == null || type == null)
                        return@let

                    val value = ModEvents.ARG_PREF_VALUE.let { key ->
                        when (Class.forName(type).kotlin) {
                            Boolean::class -> args.getBoolean(key)
                            String::class -> args.getString(key)
                            Int::class -> args.getInt(key)
                            Long::class -> args.getLong(key)
                            Float::class -> args.getFloat(key)
                            Set::class -> args.getStringArray(key)?.toSet()
                            else -> null
                        }
                    }

                    eventListener.forEach {
                        it.onSetPreferenceRequested(
                            PreferenceGroups.valueOf(groupName),
                            key,
                            Class.forName(type).kotlin,
                            value
                        )
                    }
                }
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

            Log.e("Received $ACTION intent has no extras")
            return null
        }

        fun ModEvents.createIntent(sender: KClass<*>, args: Bundle? = null) =
            Intent(ACTION).apply {
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                `package` = BuildConfig.APPLICATION_ID
                putExtra(EXTRA_EVENT, this@createIntent)
                putExtra(EXTRA_SENDER, sender.qualifiedName)
                args?.let { putExtra(EXTRA_ARGS, it) }
            }

        fun <T : Any> createEventIntent(event: ModEvents, sender: T, args: Bundle? = null) =
            event.createIntent(sender::class, args)

        fun Context.sendEvent(event: ModEvents, sender: KClass<*>, args: Bundle? = null) =
            sendBroadcast(event.createIntent(sender, args))

        inline fun <reified T> Context.sendEvent(event: ModEvents, args: Bundle? = null) =
            sendEvent(event, T::class, args)
    }
}
package me.timschneeberger.onyxtweaks.mods.base

import android.os.Bundle
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver.Companion.createEventIntent
import me.timschneeberger.onyxtweaks.bridge.ModEvents
import me.timschneeberger.onyxtweaks.bridge.OnModEventReceived
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.XPreferences
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

/**
 * Base class for all mod packs.
 *
 * @property group the preference group this mod pack belongs to
 */
abstract class ModPack : OnModEventReceived {
    abstract val group: PreferenceGroups

    val targetPackages by lazy { getTargetPackages(this::class) }
    val preferences by lazy { XPreferences(group) }

    override var modEventReceiver: ModEventReceiver? = null

    fun sendEvent(event: ModEvents, extras: Bundle? = null) {
        EzXHelper.appContext.sendBroadcast(createEventIntent(event, extras))
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
    open fun onPreferencesChanged(key: String?) {}

    /**
     * Handle the loading of a package DEX code.
     * When this method is called, EzXHelper.appContext is available
     *
     * @param lpParam load package parameters
     */
    open fun handleLoadPackage(lpParam: LoadPackageParam) {}

    /**
     * Handle the initialization of package resources.
     * Optional.
     *
     * @param param the initialization parameters
     */
    open fun handleInitPackageResources(param: InitPackageResourcesParam) {}

    companion object {
        fun getTargetPackages(modPackCls: KClass<*>): Array<String> =
            modPackCls.findAnnotations<TargetPackages>()
                .fold(arrayOf<String>()) { acc, annotation -> acc + annotation.targets }
    }
}
package me.timschneeberger.onyxtweaks.mods.base

import android.os.Bundle
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.receiver.ModEventReceiver
import me.timschneeberger.onyxtweaks.receiver.ModEvents
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.XPreferences
import kotlin.reflect.KClass

abstract class ModPack {
    abstract val group: PreferenceGroups

    val targetPackages by lazy { getTargetPackages(this::class) }
    val preferences by lazy { XPreferences(group).also {
        it.onPreferencesChanged = ::onPreferencesChanged
    }}

    fun sendBroadcast(event: ModEvents, extras: Bundle? = null) {
        EzXHelper.appContext.sendBroadcast(ModEventReceiver.createIntent(event, extras))
    }

    /**
     * Called when a preference is changed.
     *
     * @param key the key of the preference that was changed
     *            or null if all preferences were changed during initialization
     */
    open fun onPreferencesChanged(key: String?) {
        Log.dx("Preference changed: $key")
    }

    /**
     * Handle the loading of a package.
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
            modPackCls.annotations
                .mapNotNull { it as? TargetPackages }
                .fold(arrayOf<String>()) { acc, annotation -> acc + annotation.targets }
    }
}
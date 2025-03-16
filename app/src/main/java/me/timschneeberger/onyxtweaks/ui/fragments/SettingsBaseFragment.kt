package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.transition.MaterialSharedAxis
import me.timschneeberger.onyxtweaks.OnyxTweakApp
import me.timschneeberger.onyxtweaks.ui.utils.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.WorldReadableDataStore
import me.timschneeberger.onyxtweaks.ui.utils.setBackgroundFromAttribute
import kotlin.reflect.full.findAnnotations

abstract class SettingsBaseFragment : PreferenceFragmentCompat() {
    protected val app
        get() = activity?.application as? OnyxTweakApp?

    private val group by lazy {
        this::class.findAnnotations(PreferenceGroup::class).firstOrNull()?.group
            ?: throw IllegalStateException("No PreferenceGroup annotation found on ${this::class.simpleName}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            setBackgroundFromAttribute(android.R.attr.windowBackground)
        }
    }

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = WorldReadableDataStore(requireContext(), group)
        setPreferencesFromResource(group.xmlRes, rootKey)
    }
}
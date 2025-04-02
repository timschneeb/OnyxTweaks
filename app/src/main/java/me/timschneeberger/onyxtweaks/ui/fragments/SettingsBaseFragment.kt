package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.PluralsRes
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.kyuubiran.ezxhelper.Log
import com.google.android.material.transition.MaterialSharedAxis
import me.timschneeberger.onyxtweaks.OnyxTweakApp
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver.Companion.sendEvent
import me.timschneeberger.onyxtweaks.bridge.ModEvents
import me.timschneeberger.onyxtweaks.ui.activities.BasePreferenceActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.preferences.WorldReadableDataStore
import me.timschneeberger.onyxtweaks.ui.utils.setBackgroundFromAttribute
import me.timschneeberger.onyxtweaks.ui.utils.showAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import kotlin.reflect.full.findAnnotations

abstract class SettingsBaseFragment<T> : PreferenceFragmentCompat() where T : BasePreferenceActivity {
    protected val app
        get() = activity?.application as? OnyxTweakApp?

    @Suppress("UNCHECKED_CAST")
    protected val parentActivity
        get() = activity as T?

    protected val group by lazy {
        this::class.findAnnotations(PreferenceGroup::class).firstOrNull()?.group
            ?: throw IllegalStateException("No PreferenceGroup annotation found on ${this::class.simpleName}")
    }

    protected val dataStore: WorldReadableDataStore
        get() = preferenceManager.preferenceDataStore as WorldReadableDataStore

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

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Don't set up saving/loading when no group is specified
        var dataStore: WorldReadableDataStore? = null
        if (group != PreferenceGroups.NONE) {
            try {
                dataStore = WorldReadableDataStore(requireContext(), group)
            }
            catch (e: SecurityException) {
                Log.e(e)
                requireContext().showAlert(R.string.xsp_init_failed, R.string.xsp_init_failed_summary)
                // Init without world readable access to prevent immediate crash in non-LSPosed environments
                dataStore = WorldReadableDataStore(requireContext(), group, 0)
            }
            preferenceManager.preferenceDataStore = dataStore
        }

        setPreferencesFromResource(group.xmlRes, rootKey)
        onConfigurePreferences()

        dataStore?.onDataStoreModified = ::onPreferenceChanged
    }

    protected open fun onConfigurePreferences() {}

    @CallSuper
    protected open fun onPreferenceChanged(key: String) {
        requireContext().sendEvent(ModEvents.PREFERENCE_CHANGED, this::class, Bundle().apply {
            putString(ModEvents.ARG_PREF_GROUP, group.name)
            putString(ModEvents.ARG_PREF_KEY, key)
        })
    }

    protected fun requestPackageRestart(packageName: String) =
        parentActivity?.requestPackageRestart(packageName)

    protected fun EditTextPreference.configureAsNumberInput(min: Int, max: Int, @PluralsRes unitRes: Int) {
        summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            val value = preference.text?.toIntOrNull()
            when {
                value == null -> getString(R.string.value_not_set)
                else -> context.resources.getQuantityString(unitRes, value, value)
            }
        }

        dialogTitle = "$title ($min ~ ${context.resources.getQuantityString(unitRes, max, max)})"

        setOnBindEditTextListener { editText ->
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"))
        }

        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString().toIntOrNull()
            if (value == null || value < min || value > max) {
                requireContext().showAlert(
                    getString(R.string.error_invalid_input),
                    getString(R.string.error_invalid_number_out_of_range, min, max)
                )
                false
            } else {
                true
            }
        }
    }

    protected fun MultiSelectListPreference.configureAsMultiSelectInput() {
        summaryProvider = Preference.SummaryProvider<MultiSelectListPreference> { preference ->
            preference.values.count().let { count ->
                context.resources.getQuantityString(R.plurals.items_selected, count, count)
            }
        }
    }
}
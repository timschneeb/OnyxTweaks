package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.PluralsRes
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.kyuubiran.ezxhelper.Log
import com.google.android.material.transition.MaterialSharedAxis
import me.timschneeberger.onyxtweaks.OnyxTweakApp
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.preferences.WorldReadableDataStore
import me.timschneeberger.onyxtweaks.ui.utils.setBackgroundFromAttribute
import me.timschneeberger.onyxtweaks.ui.utils.showAlert
import kotlin.reflect.full.findAnnotations

abstract class SettingsBaseFragment : PreferenceFragmentCompat() {
    protected val app
        get() = activity?.application as? OnyxTweakApp?

    protected val settingsActivity
        get() = activity as? SettingsActivity?

    protected val group by lazy {
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

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        var dataStore: WorldReadableDataStore
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
        setPreferencesFromResource(group.xmlRes, rootKey)
        onConfigurePreferences()

        dataStore.onDataStoreModified = ::onPreferenceChanged
    }

    open fun onConfigurePreferences() {}
    open fun onPreferenceChanged(key: String) {}

    protected fun requestPackageRestart(packageName: String) =
        settingsActivity?.requestPackageRestart(packageName)

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
                    getString(R.string.error_invalid_number_message, min, max)
                )
                false
            } else {
                true
            }
        }
    }

    protected fun MultiSelectListPreference.configureAsMultiSelectInput() {
        summaryProvider = Preference.SummaryProvider<MultiSelectListPreference> { preference ->
            val values = preference.values
            val entryValues = preference.entryValues
            val selectedEntries = preference.entries.filterIndexed {
                    index, _ -> entryValues.contains(values.elementAt(index))
            }
            selectedEntries.joinToString()
        }
    }
}
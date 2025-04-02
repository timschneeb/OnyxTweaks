package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.utils.StringFormatter
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.ui.utils.showAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.STATUS_BAR_CLOCK)
class SettingsStatusBarClockFragment : SettingsBaseFragment<SettingsActivity>() {

    private val formatDocs by lazy { findPreference<Preference>(getString(R.string.key_status_bar_date_custom_string_documentation))!! }
    private val customTextBeforeClock by lazy { findPreference<EditTextPreference>(getString(R.string.key_status_bar_date_custom_date_before))!! }
    private val customTextAfterClock by lazy { findPreference<EditTextPreference>(getString(R.string.key_status_bar_date_custom_date_after))!! }

    override fun onConfigurePreferences() {
        super.onConfigurePreferences()

        formatDocs.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            requireContext().showAlert(
                R.string.string_formatter_manual_title,
                R.string.string_formatter_manual_body
            )
            true
        }

        customTextBeforeClock.onPreferenceChangeListener = onCustomStringChanged
        customTextAfterClock.onPreferenceChangeListener = onCustomStringChanged
    }

    override fun onPreferenceChanged(key: String) {
        super.onPreferenceChanged(key)
        requestPackageRestart(SYSTEM_UI_PACKAGE)
    }

    private val onCustomStringChanged = Preference.OnPreferenceChangeListener { preference, newValue ->
        when (preference.key) {
            getString(R.string.key_status_bar_date_custom_date_before) -> previewCustomText(newValue as String?)
            getString(R.string.key_status_bar_date_custom_date_after) -> previewCustomText(newValue as String?)
        }
        true
    }

    private fun previewCustomText(format: String?) {
        if (!format.isNullOrBlank())
            requireContext().toast(StringFormatter.formatString(format).toString())
    }
}
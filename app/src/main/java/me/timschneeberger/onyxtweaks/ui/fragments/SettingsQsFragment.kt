package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.EditTextPreference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.QS)
class SettingsQsFragment : SettingsBaseFragment() {
    private val gridRows by lazy { findPreference<EditTextPreference>(getString(R.string.key_qs_grid_row_count)) }
    private val gridColumns by lazy { findPreference<EditTextPreference>(getString(R.string.key_qs_grid_column_count)) }

    override fun onConfigurePreferences() {
        gridRows?.configureAsNumberInput(1, 10, R.plurals.unit_rows)
        gridColumns?.configureAsNumberInput(1, 10, R.plurals.unit_columns)
    }

    override fun onPreferenceChanged(key: String) {
        requestPackageRestart(SYSTEM_UI_PACKAGE)
    }
}
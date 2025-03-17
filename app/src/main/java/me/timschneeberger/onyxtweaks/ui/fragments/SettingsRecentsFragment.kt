package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.EditTextPreference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.RECENTS)
class SettingsRecentsFragment : SettingsBaseFragment() {
    private val gridRowsPortrait by lazy { findPreference<EditTextPreference>(getString(R.string.key_recents_grid_row_count_portrait)) }
    private val gridColumnsPortrait by lazy { findPreference<EditTextPreference>(getString(R.string.key_recents_grid_column_count_portrait)) }
    private val gridRowsLandscape by lazy { findPreference<EditTextPreference>(getString(R.string.key_recents_grid_row_count_landscape)) }
    private val gridColumnsLandscape by lazy { findPreference<EditTextPreference>(getString(R.string.key_recents_grid_column_count_landscape)) }
    private val gridSpacing by lazy { findPreference<EditTextPreference>(getString(R.string.key_recents_grid_spacing)) }

    override fun onConfigurePreferences() {
        gridRowsPortrait?.configureAsNumberInput(1, 16, R.plurals.unit_rows)
        gridColumnsPortrait?.configureAsNumberInput(1, 16, R.plurals.unit_columns)
        gridRowsLandscape?.configureAsNumberInput(1, 16, R.plurals.unit_rows)
        gridColumnsLandscape?.configureAsNumberInput(1, 16, R.plurals.unit_columns)
        gridSpacing?.configureAsNumberInput(0, 300, R.plurals.unit_pixels)
    }

    override fun onPreferenceChanged(key: String) {
        requestPackageRestart(SYSTEM_UI_PACKAGE)
    }
}
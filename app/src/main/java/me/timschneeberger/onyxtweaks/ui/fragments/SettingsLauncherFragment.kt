package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity.Companion.ZYGOTE_MARKER
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartLauncher
import me.timschneeberger.onyxtweaks.ui.utils.showYesNoAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences


@PreferenceGroup(PreferenceGroups.LAUNCHER)
class SettingsLauncherFragment : SettingsBaseFragment() {
    private val desktopReInit by lazy { findPreference<Preference>(getString(R.string.key_launcher_desktop_reinit)) }
    private val desktopRows by lazy { findPreference<EditTextPreference>(getString(R.string.key_launcher_desktop_row_count)) }
    private val desktopColumns by lazy { findPreference<EditTextPreference>(getString(R.string.key_launcher_desktop_column_count)) }
    private val desktopDockColumns by lazy { findPreference<EditTextPreference>(getString(R.string.key_launcher_desktop_dock_column_count)) }
    private val barHiddenItems by lazy { findPreference<MultiSelectListPreference>(getString(R.string.key_launcher_bar_hidden_items)) }
    private val settingsAddedShortcuts by lazy { findPreference<MultiSelectListPreference>(getString(R.string.key_launcher_settings_added_shortcuts)) }

    override fun onConfigurePreferences() {
        desktopReInit?.setOnPreferenceClickListener {
            requireContext().showYesNoAlert(
                R.string.launcher_desktop_reinit_confirm_title,
                R.string.launcher_desktop_reinit_confirm_message,
                R.string.continue_action,
                R.string.cancel
            ) {
                if (it) {
                    // Raise flag to reinitialize the launcher
                    // Will be handled by DesktopGridSize on next launcher boot
                    Preferences(requireContext(), group).set(R.string.key_launcher_reinit_flag, true)
                    requireContext().restartLauncher()
                }
            }
            true
        }

        desktopRows?.configureAsNumberInput(2, 16, R.plurals.unit_rows)
        desktopColumns?.configureAsNumberInput(2, 16, R.plurals.unit_columns)
        desktopDockColumns?.configureAsNumberInput(1, 16, R.plurals.unit_columns)

        barHiddenItems?.configureAsMultiSelectInput()
        settingsAddedShortcuts?.configureAsMultiSelectInput()
    }

    override fun onPreferenceChanged(key: String) {
        requestPackageRestart(
            when (key) {
                getString(R.string.key_launcher_desktop_wallpaper) -> ZYGOTE_MARKER
                else -> LAUNCHER_PACKAGE
            }
        )
    }
}
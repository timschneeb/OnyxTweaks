package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.showYesNoAlert
import me.timschneeberger.onyxtweaks.ui.utils.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences
import me.timschneeberger.onyxtweaks.utils.restartLauncher


@PreferenceGroup(PreferenceGroups.LAUNCHER)
class SettingsLauncherFragment : SettingsBaseFragment() {
    private val desktopReInit by lazy { findPreference<Preference>(getString(R.string.key_launcher_desktop_reinit)) }

    override fun onConfigurePreferences() {
        desktopReInit?.setOnPreferenceClickListener {
            requireContext().showYesNoAlert(
                R.string.launcher_desktop_reinit_confirm_title,
                R.string.launcher_desktop_reinit_confirm_message
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
    }
}
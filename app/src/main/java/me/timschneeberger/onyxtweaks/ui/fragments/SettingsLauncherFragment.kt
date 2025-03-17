package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity.Companion.ZYGOTE_MARKER
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartLauncher
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.showYesNoAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences


@PreferenceGroup(PreferenceGroups.LAUNCHER)
class SettingsLauncherFragment : SettingsBaseFragment() {
    private val desktopReInit by lazy { findPreference<Preference>(getString(R.string.key_launcher_desktop_reinit)) }

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
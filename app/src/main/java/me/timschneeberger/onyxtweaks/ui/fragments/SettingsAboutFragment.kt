package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.ABOUT)
class SettingsAboutFragment : SettingsBaseFragment<SettingsActivity>() {
    private val version by lazy { findPreference<Preference>(getString(R.string.key_credits_version)) }
    private val buildInfo by lazy { findPreference<Preference>(getString(R.string.key_credits_build_info)) }

    override fun onConfigurePreferences() {
        val type = getString(
            if (BuildConfig.DEBUG) R.string.credits_debug_build
            else R.string.credits_release_build
        )

        version?.summary = BuildConfig.VERSION_NAME
        buildInfo?.summary = getString(
            R.string.credits_build_info_summary,
            type,
            BuildConfig.COMMIT_SHA,
            BuildConfig.BUILD_TIME
        )
    }
}
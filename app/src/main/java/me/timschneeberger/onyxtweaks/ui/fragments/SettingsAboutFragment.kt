package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.utils.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.ABOUT)
class SettingsAboutFragment : SettingsBaseFragment() {

    private val version by lazy { findPreference<Preference>(getString(R.string.key_credits_version)) }
    private val buildInfo by lazy { findPreference<Preference>(getString(R.string.key_credits_build_info)) }

    override fun onConfigurePreferences() {
        val type = if(BuildConfig.DEBUG) "Debug" else "Release"
        version?.summary = BuildConfig.VERSION_NAME
        buildInfo?.summary = "$type build @${BuildConfig.COMMIT_SHA} (compiled at ${BuildConfig.BUILD_TIME})"
    }
}
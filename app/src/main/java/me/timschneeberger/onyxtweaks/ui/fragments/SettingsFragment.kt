package me.timschneeberger.onyxtweaks.ui.fragments

import android.content.Intent
import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.activities.ConfigEditorActivity
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.ROOT)
class SettingsFragment : SettingsBaseFragment<SettingsActivity>() {
    private val mmkv by lazy { findPreference<Preference>(getString(R.string.key_mmkv_editor)) }

    override fun onConfigurePreferences() {
        mmkv?.setOnPreferenceClickListener {
            requireContext().startActivity(Intent(requireContext(), ConfigEditorActivity::class.java))
            true
        }
    }
}
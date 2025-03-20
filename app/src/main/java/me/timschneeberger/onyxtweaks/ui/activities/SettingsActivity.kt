package me.timschneeberger.onyxtweaks.ui.activities

import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.fragments.SettingsFragment

class SettingsActivity : BasePreferenceActivity() {
    override val rootTitleRes: Int = R.string.app_name
    override val rootSubtitleRes: Int = R.string.module_description
    override val rootIsSubActivity: Boolean = false

    override fun createRootFragment() = SettingsFragment()
}
package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.github.kyuubiran.ezxhelper.Log
import com.topjohnwu.superuser.Shell
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.global.PerActivityRefreshModes
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.model.ActivityInfo
import me.timschneeberger.onyxtweaks.ui.model.ActivityItemViewModel
import me.timschneeberger.onyxtweaks.ui.model.ActivityRule
import me.timschneeberger.onyxtweaks.ui.model.AppInfo
import me.timschneeberger.onyxtweaks.ui.model.AppItemViewModel
import me.timschneeberger.onyxtweaks.ui.preferences.DeletablePreference
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.ui.utils.showSingleChoiceAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.PER_ACTIVITY_SETTINGS)
class PerActivitySettingsFragment : SettingsBaseFragment<SettingsActivity>() {

    private lateinit var appsListFragment: AppsListFragment
    private val appListViewModel by viewModels<AppItemViewModel>()

    private lateinit var activityListFragment: ActivityListFragment
    private val activityListViewModel by viewModels<ActivityItemViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            appsListFragment = AppsListFragment()
            appListViewModel.selectedItem.observe(viewLifecycleOwner, ::onAppSelected)
            activityListViewModel.selectedItem.observe(viewLifecycleOwner, ::onActivitySelected)
        }
    }

    override fun onConfigurePreferences() {
        PreferenceCategory(requireContext()).apply {
            title = getString(R.string.per_activity_settings_actions)
            isIconSpaceReserved = false
        }.let { root ->
            preferenceScreen.addPreference(root)

            root.addPreference(
                Preference(requireContext()).apply {
                    setIcon(R.drawable.ic_twotone_error_24dp)
                    summary = getString(R.string.per_activity_settings_scope_hint)
                    isIconSpaceReserved = false
                }
            )

            root.addPreference(
                Preference(requireContext()).apply {
                    setIcon(R.drawable.ic_baseline_open_in_new_24dp)
                    title = getString(R.string.per_activity_settings_lsposed_scopes)
                    summary = getString(R.string.per_activity_settings_lsposed_scopes_summary)
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        Shell.getShell { shell ->
                            shell.newJob().add(
                                "am start-activity -a android.intent.action.MAIN -p com.android.shell -n com.android.shell/.BugreportWarningActivity " +
                                        "-c org.lsposed.manager.LAUNCH_MANAGER -d module://${BuildConfig.APPLICATION_ID}:`am get-current-user`"
                            ).exec()
                        }
                        true
                    }
                }
            )

            root.addPreference(
                Preference(requireContext()).apply {
                    setIcon(R.drawable.ic_twotone_add_circle_24dp)
                    title = getString(R.string.per_activity_settings_add_app)
                    summary = getString(R.string.per_activity_settings_add_app_summary)
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        showAppSelector()
                        true
                    }
                }
            )
        }

        readRules().groupBy(ActivityRule::packageName).forEach { (packageName, rules) ->
            PreferenceCategory(requireContext()).apply {
                title = rules.first().appName
                summary = packageName
                isIconSpaceReserved = false
            }.let { root ->
                preferenceScreen.addPreference(root)

                root.addPreference(
                    Preference(requireContext()).apply {
                        setIcon(R.drawable.ic_twotone_library_add_24dp)
                        title = getString(R.string.per_activity_settings_add_activity)
                        summary = getString(R.string.per_activity_settings_add_activity_summary)
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            activityListFragment = ActivityListFragment(rules.first().packageName)
                            showActivitySelector()
                            true
                        }
                    }
                )

                rules.forEach { rule ->
                    root.addPreference(
                        DeletablePreference(requireContext()).apply {
                            if (rule.activityClass == null)
                                setIcon(R.drawable.ic_twotone_more_horiz_24)

                            title = rule.activityClass ?: getString(R.string.per_activity_settings_scope_all_activities)
                            summary = getString(
                                R.string.per_activity_settings_activity_mode_summary,
                                rule.updateMode
                            )
                            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                                requireContext().showSingleChoiceAlert(
                                    getString(
                                        R.string.per_activity_settings_change_mode_title,
                                        rule.activityName ?: getString(R.string.per_activity_settings_change_mode_title_other_items)
                                    ),
                                    PerActivityRefreshModes.UpdateOption
                                        .entries
                                        .map { it.name as CharSequence }
                                        .toTypedArray(),
                                    rule.updateMode.ordinal
                                ) { index ->
                                    index ?: return@showSingleChoiceAlert

                                    val newRules = readRules().toMutableList()
                                    newRules[newRules.indexOf(rule)] = rule.copy(updateMode = PerActivityRefreshModes.UpdateOption.entries[index])
                                    saveRules(newRules)
                                    refreshList()
                                }
                                true
                            }

                            onDeleteClicked = {
                                val newRules = readRules().toMutableList()
                                if (rule.activityClass == null)
                                    newRules.removeAll { it.packageName == rule.packageName }
                                else
                                    newRules.remove(rule)
                                saveRules(newRules)
                                refreshList()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun onAppSelected(appInfo: AppInfo) {
        addRule(ActivityRule.fromApp(appInfo.packageName, appInfo.appName))
    }

    private fun onActivitySelected(activityInfo: ActivityInfo) {
        addRule(ActivityRule.fromActivityInfo(requireContext(), activityInfo))
    }

    private fun addRule(rule: ActivityRule) {
        val rules = readRules().toMutableList()
        if(rules.any { it.packageName == rule.packageName && it.activityClass == rule.activityClass }) {
            requireContext().toast(getString(R.string.entry_already_exists))
            return
        }

        rules.add(rule)
        saveRules(rules)
        refreshList()
    }

    private fun saveRules(rules: List<ActivityRule>) {
        dataStore.putString(getString(R.string.key_per_activity_settings), Json.encodeToString(rules))
    }

    private fun readRules(): List<ActivityRule> {
        return try {
            Json.decodeFromString<List<ActivityRule>>(
                dataStore.getString(getString(R.string.key_per_activity_settings), null) ?: "[]"
            )
        }
        catch (ex: SerializationException) {
            Log.e(ex, "Failed to deserialize activity rules")
            emptyList<ActivityRule>()
        }
    }

    private fun showAppSelector() {
        if(!appsListFragment.isAdded)
            appsListFragment.show(childFragmentManager, AppsListFragment::class.java.name)
    }

    private fun showActivitySelector() {
        if(!activityListFragment.isAdded)
            activityListFragment.show(childFragmentManager, ActivityListFragment::class.java.name)
    }

    private fun refreshList() {
        val scrollY = listView.scrollY

        preferenceScreen.removeAll()
        onConfigurePreferences()

        listView.scrollY = scrollY
    }
}
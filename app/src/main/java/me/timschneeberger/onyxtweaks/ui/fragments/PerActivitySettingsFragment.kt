package me.timschneeberger.onyxtweaks.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.github.kyuubiran.ezxhelper.Log
import com.onyx.android.sdk.api.device.epd.UpdateMode
import com.topjohnwu.superuser.Shell
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.DialogPerActivitySettingsEditBinding
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.model.ActivityInfo
import me.timschneeberger.onyxtweaks.ui.model.ActivityItemViewModel
import me.timschneeberger.onyxtweaks.ui.model.ActivityRule
import me.timschneeberger.onyxtweaks.ui.model.AppInfo
import me.timschneeberger.onyxtweaks.ui.model.AppItemViewModel
import me.timschneeberger.onyxtweaks.ui.preferences.DeletablePreference
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
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
                    summary = getString(R.string.xposed_scope_hint)
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
                            summary = rule.updateMethod.toString()
                            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                                showEditDialog(rule)
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
            emptyList()
        }
    }

    private fun showEditDialog(rule: ActivityRule) {
        val content = DialogPerActivitySettingsEditBinding.inflate(layoutInflater).apply {
            updateMode.setAdapter(CustomSimpleAdapter(
                requireContext(),
                UpdateMode.entries.map {
                    linkedMapOf(
                        "name" to it.name,
                        "desc" to getString(updateModeSummaries.getOrElse(it) { R.string.update_mode_undocumented_summary })
                    )
                },
                android.R.layout.simple_list_item_2,
                arrayOf("name", "desc"),
                intArrayOf(android.R.id.text1, android.R.id.text2)
            ))
            updateMode.setText(rule.updateMethod.name, false)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(
                R.string.per_activity_settings_change_mode_title,
                rule.activityName ?: getString(R.string.per_activity_settings_change_mode_title_other_items)
            ))
            .setView(content.root)
            .setPositiveButton(android.R.string.ok) { inputDialog, _ ->
                val newUpdateMode = try {
                    UpdateMode.valueOf(content.updateMode.text.toString())
                }
                catch (_: IllegalArgumentException) {
                    UpdateMode.None
                }

                val newRules = readRules().toMutableList()
                newRules[newRules.indexOf(rule)] = rule.copy(updateMethod = newUpdateMode)
                saveRules(newRules)
                refreshList()
            }
            .setNegativeButton(android.R.string.cancel) {_, _ -> }
            .create()
            .show()
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

    private class CustomSimpleAdapter(
        context: Context,
        data: List<Map<String, Any>>,
        layout: Int,
        from: Array<String>,
        to: IntArray
    ) : SimpleAdapter(context, data, layout, from, to) {
        override fun getItem(position: Int): Any {
            val map = super.getItem(position) as LinkedHashMap<*, *>
            return map.entries.first { entry -> entry.key == "name" }.value
        }
    }

    companion object {
        val updateModeSummaries = UpdateMode.entries.associate { mode ->
            mode to when (mode) {
                UpdateMode.None -> R.string.update_mode_none_summary
                UpdateMode.DU -> R.string.update_mode_du_summary
                UpdateMode.DU4 -> R.string.update_mode_du4_summary
                UpdateMode.GU -> R.string.update_mode_gu_summary
                UpdateMode.GU_FAST -> R.string.update_mode_gu_fast_summary
                UpdateMode.GC -> R.string.update_mode_gc_summary
                UpdateMode.ANIMATION -> R.string.update_mode_animation_summary
                UpdateMode.ANIMATION_QUALITY -> R.string.update_mode_animation_quality_summary
                UpdateMode.ANIMATION_X -> R.string.update_mode_animation_x_summary
                UpdateMode.GC4 -> R.string.update_mode_gc4_summary
                UpdateMode.REGAL -> R.string.update_mode_regal_summary
                UpdateMode.REGAL_D -> R.string.update_mode_regal_d_summary
                UpdateMode.REGAL_PLUS -> R.string.update_mode_regal_plus_summary
                UpdateMode.DU_QUALITY -> R.string.update_mode_du_quality_summary
                else -> R.string.update_mode_undocumented_summary
            }
        }
    }
}
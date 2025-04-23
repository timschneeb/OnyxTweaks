package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.github.kyuubiran.ezxhelper.Log
import kotlinx.serialization.SerializationException
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.model.AppInfo
import me.timschneeberger.onyxtweaks.ui.model.AppItemViewModel
import me.timschneeberger.onyxtweaks.ui.preferences.DeletablePreference
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.getAppName
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.RESUME_APP_SETTINGS)
class ResumeActivitySettingsFragment : SettingsBaseFragment<SettingsActivity>() {

    private lateinit var appsListFragment: AppsListFragment
    private val appListViewModel by viewModels<AppItemViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            appsListFragment = AppsListFragment()
            appListViewModel.selectedItem.observe(viewLifecycleOwner, ::onAppSelected)
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
                    setIcon(R.drawable.ic_twotone_info_24dp)
                    summary = getString(R.string.misc_resume_app_hint)
                    isIconSpaceReserved = false
                }
            )

            root.addPreference(
                Preference(requireContext()).apply {
                    setIcon(R.drawable.ic_twotone_add_circle_24dp)
                    title = getString(R.string.misc_resume_app_add)
                    summary = getString(R.string.misc_resume_app_add_summary)
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        showAppSelector()
                        true
                    }
                }
            )

            root.addPreference(
                EditTextPreference(requireContext()).apply {
                    setIcon(R.drawable.ic_twotone_timer_24dp)
                    setDefaultValue(resources.getInteger(R.integer.default_resume_app_delay_seconds).toString())

                    key = getString(R.string.key_resume_app_delay_seconds)
                    title = getString(R.string.misc_resume_app_delay_seconds)
                }.apply {
                    configureAsNumberInput(1, 300, R.plurals.unit_seconds)
                }
            )
        }

        PreferenceCategory(requireContext()).apply {
            title = getString(R.string.misc_resume_app_allowed_category)
            isIconSpaceReserved = false
        }.let { root ->
            readPackages().forEach { pkgName ->
                preferenceScreen.addPreference(root)
                root.addPreference(
                    DeletablePreference(requireContext()).apply {
                        title = requireContext().getAppName(pkgName)
                        summary = pkgName
                        isIconSpaceReserved = false

                        onDeleteClicked = {
                            readPackages()
                                .toMutableSet()
                                .apply { remove(pkgName) }
                                .let(::savePackages)
                            refreshList()
                        }
                    }
                )
            }
        }
    }

    private fun onAppSelected(appInfo: AppInfo) {
        addPackage(appInfo.packageName)
    }

    private fun addPackage(rule: String) {
        val packages = readPackages().toMutableSet()
        if(packages.contains(rule)) {
            requireContext().toast(getString(R.string.entry_already_exists))
            return
        }

        packages.add(rule)
        savePackages(packages)
        refreshList()
    }

    private fun savePackages(packages: Set<String>) {
        dataStore.putStringSet(getString(R.string.key_resume_app_rules), packages)
    }

    private fun readPackages(): Set<String> {
        return try {
            dataStore.getStringSet(getString(R.string.key_resume_app_rules), null) ?: emptySet()
        }
        catch (ex: SerializationException) {
            Log.e(ex, "Failed to deserialize activity rules")
            emptySet()
        }
    }

    private fun showAppSelector() {
        if(!appsListFragment.isAdded)
            appsListFragment.show(childFragmentManager, AppsListFragment::class.java.name)
    }

    private fun refreshList() {
        val scrollY = listView.scrollY

        preferenceScreen.removeAll()
        onConfigurePreferences()

        listView.scrollY = scrollY
    }
}
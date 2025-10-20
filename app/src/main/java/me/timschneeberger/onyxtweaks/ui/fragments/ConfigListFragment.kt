package me.timschneeberger.onyxtweaks.ui.fragments

import android.content.pm.PackageManager
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.ConfigEditorActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.CompatExtensions.getApplicationInfoCompat
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.killPackage
import me.timschneeberger.onyxtweaks.ui.utils.showAlert
import me.timschneeberger.onyxtweaks.ui.utils.showSingleChoiceAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.NONE)
class ConfigListFragment : SettingsBaseFragment<ConfigEditorActivity>() {
    private val onyxApps = arrayOf(
        LAUNCHER_PACKAGE,
        "com.onyx.dict",
        "com.onyx.kime",
        "com.onyx.mail",
        "com.onyx.android.ksync",
        "com.onyx.easytransfer",
        "com.onyx.floatingbutton",
        "com.onyx.gallery",
        "com.onyx.kreader",
        "com.onyx.aiassistant",
        "com.onyx.appmarket",
        "com.onyx.android.note"
    )

    override fun onConfigurePreferences() {
        PreferenceCategory(requireContext()).apply {
            title = getString(R.string.mmkv_list_select_data_store)
            isIconSpaceReserved = false
        }.let { root ->
            preferenceScreen.addPreference(root)

            root.addPreference(
                Preference(requireContext()).apply {
                    title = getString(R.string.mmkv_list_system_config)
                    summary = getString(R.string.mmkv_list_system_config_summary)
                    isIconSpaceReserved = false
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        navigateToSystemEditor()
                        true
                    }
                }
            )

            onyxApps.mapNotNull { pkg ->
                val appInfo = try {
                    requireContext().packageManager.getApplicationInfoCompat(pkg)
                } catch (_: PackageManager.NameNotFoundException) {
                    return@mapNotNull null
                }

                Preference(requireContext()).apply {
                    title = appInfo.loadLabel(requireContext().packageManager).toString()
                    summary =
                        getString(R.string.mmkv_list_private_config_summary, appInfo.packageName)
                    isIconSpaceReserved = false
                    onPreferenceClickListener = Preference.OnPreferenceClickListener { pref ->
                        if (parentActivity?.isNonRootMode == true) {
                            requireContext().showAlert(
                                R.string.mmkv_list_needs_root,
                                R.string.mmkv_list_needs_root_message
                            )
                            return@OnPreferenceClickListener false
                        }

                        val ids = parentActivity
                            ?.mmkvService
                            ?.findDataStoresForPackage(pkg)
                            ?.filterNotNull()
                            ?.map { it as CharSequence }
                            ?.toTypedArray()

                        if (ids == null) {
                            requireContext().showAlert(R.string.mmkv_list_service_unavailable, R.string.mmkv_list_service_unavailable_message)
                            return@OnPreferenceClickListener false
                        }

                        if (ids.isEmpty()) {
                            requireContext().showAlert(R.string.mmkv_list_no_config, R.string.mmkv_list_no_config_message)
                            return@OnPreferenceClickListener false
                        }

                        if (ids.size == 1) {
                            navigateToEditor(pkg, ids.first())
                            return@OnPreferenceClickListener true
                        }

                        requireContext().showSingleChoiceAlert(
                            title = R.string.mmkv_select_mmkv_source,
                            choices = ids,
                            checkedIndex = -1
                        ) { selected ->
                            if (selected != null && selected >= 0 && selected < ids.size) {
                                navigateToEditor(pkg, ids[selected])
                            }
                        }
                        true
                    }
                }
            }.forEach(root::addPreference)
        }
    }

    private fun navigateToSystemEditor() {
        val service = parentActivity?.mmkvService
        if (service == null) {
            requireContext().showAlert(R.string.mmkv_list_service_unavailable, R.string.mmkv_list_service_unavailable_message)
            return
        }

        val handle = service.openSystem()
        if (handle == null) {
            requireContext().showAlert(R.string.mmkv_list_open_failed_system, R.string.mmkv_list_open_failed_system_message)
            return
        }

        parentActivity?.navigateToFragment(
            ConfigEditorFragment.newInstance(handle, "android"),
            "onyx_config",
            getString(R.string.mmkv_list_system_config)
        )
    }

    private fun navigateToEditor(pkg: String, mmapId: CharSequence) {
        if(pkg != "com.onyx.kime") // Don't kill the keyboard; users wouldn't be able to edit
            requireContext().killPackage(pkg)

        val handle = parentActivity?.mmkvService?.open(pkg, mmapId.toString())
        if (handle == null) {
            requireContext().showAlert(R.string.mmkv_list_load_failed, R.string.mmkv_list_load_failed_message)
            return
        }

        parentActivity?.navigateToFragment(
            ConfigEditorFragment.newInstance(handle, pkg),
            mmapId.toString(),
            pkg
        )
    }
}
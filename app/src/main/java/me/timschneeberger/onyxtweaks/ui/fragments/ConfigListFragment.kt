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
            title = "Select data store"
            isIconSpaceReserved = false
        }.let { root ->
            preferenceScreen.addPreference(root)

            root.addPreference(
                Preference(requireContext()).apply {
                    title = "System config"
                    summary = "Global data store at /onyxconfig/mmkv"
                    isIconSpaceReserved = false
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        navigateToSystemEditor()
                        true
                    }
                }
            )

            onyxApps.mapNotNull { pkg ->
                val appInfo = try {
                    requireContext().packageManager.getApplicationInfoCompat(pkg, 0)
                } catch (_: PackageManager.NameNotFoundException) {
                    return@mapNotNull null
                }

                Preference(requireContext()).apply {
                    title = appInfo.loadLabel(requireContext().packageManager).toString()
                    summary = "Private data store at /data/data/${appInfo.packageName}/files/mmkv"
                    isIconSpaceReserved = false
                    onPreferenceClickListener = Preference.OnPreferenceClickListener { pref ->
                        val ids = parentActivity
                            ?.mmkvService
                            ?.findDataStoresForPackage(pkg)
                            ?.filterNotNull()
                            ?.map { it as CharSequence }
                            ?.toTypedArray()

                        if (ids == null) {
                            requireContext().showAlert("Service unavailable", "The root service is not yet initialized. Make sure that root access has been granted and restart the app.")
                            return@OnPreferenceClickListener false
                        }

                        if (ids.isEmpty()) {
                            requireContext().showAlert("No config files", "No MMKV data stores found for this package.")
                            return@OnPreferenceClickListener false
                        }

                        if (ids.count() == 1) {
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
            requireContext().showAlert("Service unavailable", "The root service is not yet initialized. Please try again in a moment. Also make sure that root access has been granted and restart the app, if it was not granted before.")
            return
        }

        val handle = service.openSystem()
        if (handle == null) {
            requireContext().showAlert("Failed to open system config", "The system config at '/onyxconfig/mmkv/onyx_config' could not be opened. Check if the path exists and ensure root access has been granted.")
            return
        }

        parentActivity?.navigateToFragment(
            ConfigEditorFragment.newInstance(handle, "android"),
            "onyx_config",
            "System config"
        )
    }

    private fun navigateToEditor(pkg: String, mmapId: CharSequence) {
        requireContext().killPackage(pkg)

        val handle = parentActivity?.mmkvService?.open(pkg, mmapId.toString())
        if (handle == null) {
            requireContext().showAlert("Failed to open data store", "The data store could not be opened. Make sure that root access has been granted and restart the app.")
            return
        }

        parentActivity?.navigateToFragment(
            ConfigEditorFragment.newInstance(handle, pkg),
            mmapId.toString(),
            pkg
        )
    }
}
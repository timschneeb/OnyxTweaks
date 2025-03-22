package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.activities.ConfigEditorActivity
import me.timschneeberger.onyxtweaks.ui.activities.TextEditorActivity
import me.timschneeberger.onyxtweaks.ui.preferences.DeletablePreference
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.services.MMKVAccessService.Companion.SYSTEM_HANDLE
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartZygote
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils.putString
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils.resolveType
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils.resolveValue
import me.timschneeberger.onyxtweaks.ui.utils.showAlert
import me.timschneeberger.onyxtweaks.ui.utils.showInputAlert
import me.timschneeberger.onyxtweaks.ui.utils.showSingleChoiceAlert
import me.timschneeberger.onyxtweaks.ui.utils.showYesNoAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.NONE)
class ConfigEditorFragment : SettingsBaseFragment<ConfigEditorActivity>() {
    private var handle: String? = null
    private var pkg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        handle = requireArguments().getString(ARG_HANDLE)
        pkg = requireArguments().getString(ARG_PKG)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        parentActivity?.mmkvService?.close(handle)
        super.onDestroy()
    }

    override fun onConfigurePreferences() {
        PreferenceCategory(requireContext()).apply {
            title = "Special actions"
            isIconSpaceReserved = false
        }.let { root ->
            preferenceScreen.addPreference(root)


            if (handle == SYSTEM_HANDLE) {
                root.addPreference(
                    Preference(requireContext()).apply {
                        setIcon(R.drawable.ic_twotone_info_24dp)
                        summary = "IMPORTANT: After making changes, you should reboot before leaving this app. Otherwise, other system components may not pick up the changes and override them with the previous state."
                        isSelectable = false
                    }
                )

                root.addPreference(
                    Preference(requireContext()).apply {
                        title = "Apply & soft-reboot device"
                        summary = "Apply changes by syncing the MMKV data store and soft-rebooting the device"
                        isIconSpaceReserved = false
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            parentActivity?.restartZygote()
                            true
                        }
                    }
                )

                root.addPreference(
                    Preference(requireContext()).apply {
                        title = "Remove all app-specific EAC configuration keys"
                        summary = "Clear keys related to app-specific e-Ink optimization settings"
                        isIconSpaceReserved = false
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            requireContext().showYesNoAlert(
                                "Remove all app-specific EAC configuration keys",
                                "This will remove all keys containing a package name and starting with 'eac_app_' from the MMKV data store. This action cannot be undone.\nThe device will reinitialize all entries with the value stored in 'eac_default_app_config'.\n\nIMPORTANT: Reboot your device after performing this action, otherwise the changes will not be applied properly.",
                                getString(R.string.continue_action),
                                getString(R.string.cancel)
                            ) { confirmed ->
                                val service = parentActivity?.mmkvService
                                if(confirmed && service != null) {
                                    service.allKeys(handle)
                                        .filter { it.startsWith("eac_app_") && it.contains(".") }
                                        .toMutableList()
                                        .also { Log.d("Keys to be removed: ${it.joinToString()}") }
                                        .forEach { key -> service.remove(handle, key) }
                                    service.sync(handle)
                                    refreshList()
                                }
                            }
                            true
                        }
                    }
                )
            }
            else {
                root.addPreference(
                    Preference(requireContext()).apply {
                        title = "Add new key-value pair"
                        summary = "Insert a new key-value pair into the MMKV data store"
                        isIconSpaceReserved = false
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            requireContext().showInputAlert(
                                layoutInflater,
                                "Enter key name",
                                "Key name",
                                "",
                                false,
                                null
                            ) { key ->
                                if(key == null)
                                    return@showInputAlert

                                onAddOrEdit(key, edit = false) { _ ->
                                    refreshList()
                                }
                            }
                            true
                        }
                    }
                )
            }
        }

        PreferenceCategory(requireContext()).apply {
            title = "Key-value pairs"
            isIconSpaceReserved = false
        }.let { root ->
            preferenceScreen.addPreference(root)

            if (handle != SYSTEM_HANDLE) {
                root.addPreference(
                    Preference(requireContext()).apply {
                        setIcon(R.drawable.ic_twotone_info_24dp)
                        summary =
                            "Note: All processes of '$pkg' were automatically killed to prevent file access conflicts and ensure that your changes take effect properly."
                        isIconSpaceReserved = false
                        isSelectable = false
                    }
                )
            }

            val service = parentActivity?.mmkvService
            if(handle == null || service == null)
                return

            service.allKeys(handle)
                .apply { sort() }
                .also { strings -> Log.i("Keys: ${strings.joinToString()}") }
                .forEach { key ->
                    root.addPreference(
                        DeletablePreference(requireContext()).apply {
                            title = key
                            summary = service.resolveValue(handle, key, true)?.toString()
                            isIconSpaceReserved = false
                            onPreferenceClickListener = Preference.OnPreferenceClickListener { pref ->
                                onAddOrEdit(key, edit = true) {
                                    pref.summary = it?.toString()
                                }
                                true
                            }
                            onDeleteClicked = { pref ->
                                requireContext().showYesNoAlert(
                                    "Delete key '$key'",
                                    "This will permanently delete the key '$key' from the MMKV data store. This action cannot be undone.",
                                    getString(R.string.continue_action),
                                    getString(R.string.cancel)
                                ) { confirmed ->
                                    if(confirmed) {
                                        service.remove(handle, key)
                                        service.sync(handle)
                                        root.removePreference(pref)
                                    }
                                }
                            }
                        }
                    )
                }
        }
    }

    fun refreshList() {
        preferenceScreen.removeAll()
        onConfigurePreferences()
    }

    fun onAddOrEdit(key: String, edit: Boolean, onEdited: ((newValue: Any?) -> Unit)) {
        val service = parentActivity?.mmkvService
        if(handle == null || service == null)
            return

        val hasKnownType = MMKVUtils.isKnownType(key)
        val resolvedType = service.resolveType(handle, key)
        val currentValue by lazy {
            if (edit && resolvedType != null)
                service.resolveValue(handle, key, false, resolvedType)
            else
                null
        }

        if(hasKnownType && resolvedType != null) {
            editItemAs(key, resolvedType, currentValue, onEdited)
        }
        else {
            promptForType(resolvedType, edit) { chosenType ->
                editItemAs(key, chosenType, currentValue, onEdited)
            }
        }
    }

    fun editItemAs(key: String, type: MMKVUtils.KnownTypes, currentValue: Any?, onEdited: ((newValue: Any?) -> Unit)) {
        val service = parentActivity?.mmkvService
        if(handle == null || service == null)
            return

        Log.i("Editing key '$key' as type ${type.name::class.simpleName}. Current value: $currentValue (isNull: ${currentValue == null})")

        if(type.typeClass == String::class) {
            parentActivity?.textEditorLauncher?.launch(
                TextEditorActivity.createIntent(
                    requireContext(),
                    type.editorMode ?: MMKVUtils.EditorMode.PLAIN_TEXT,
                    handle!!,
                    key,
                    currentValue?.toString() ?: "New value",
                )
            )
            return
        }

        requireContext().showInputAlert(
            layoutInflater,
            "Edit key '$key' as ${type.description}",
            "New value",
            currentValue?.toString() ?: "",
            type.typeClass == Int::class || type.typeClass == Long::class || type.typeClass == Float::class,
            null
        ) { newValue ->
            if(newValue == null)
                return@showInputAlert

            try {
                when (type.typeClass) {
                    // TODO string & stringset editor w/ JSON support
                    String::class -> service.putString(handle, key, newValue.toString()) // service.putString(handle, key, newValue.toString())
                    Set::class -> throw UnsupportedOperationException() //service.putLongStringSet(handle, key, newValue.())
                    Int::class -> service.putInt(handle, key, newValue.toInt())
                    Long::class -> service.putLong(handle, key, newValue.toLong())
                    Float::class -> service.putFloat(handle, key, newValue.toFloat())
                    Boolean::class -> service.putBoolean(handle, key, newValue.toBoolean())
                }
                service.sync(handle)
                onEdited.invoke(newValue)
            }
            catch (e: NumberFormatException) {
                Log.e(e)
                requireContext().showAlert(
                    "Failed to save value",
                    "Invalid number format for type '${type.description}': $newValue"
                )
            }
            catch (e: Exception) {
                Log.e(e)
                requireContext().showAlert(
                    "Failed to save value",
                    "Reason:\n$e"
                )
            }
        }
    }

    fun promptForType(guessedType: MMKVUtils.KnownTypes?, edit: Boolean, onTypeSelected: ((MMKVUtils.KnownTypes) -> Unit)) {
        val availableTypes = MMKVUtils.KnownTypes.entries.map {it.description}.let {
            if (guessedType == null)
                it
            else
                it + arrayOf("Auto-detect (${guessedType.description})")
        }
            .map { it as CharSequence }
            .toTypedArray()

        requireContext().showSingleChoiceAlert(
            title = if(edit) "Unknown data type, choose manually" else "Select data type for new key",
            choices = availableTypes,
            checkedIndex = -1
        ) { idx ->
            if(idx != null && idx >= 0 && idx < availableTypes.size) {
                // We add an additional item
                val chosenType =
                    if(idx < MMKVUtils.KnownTypes.entries.size)
                        MMKVUtils.KnownTypes.entries.elementAt(idx)
                    else
                        guessedType!!

                onTypeSelected.invoke(chosenType)
            }
        }
    }

    companion object {
        private const val ARG_HANDLE = "handle"
        private const val ARG_PKG = "pkg"

        fun newInstance(handle: String, pkg: String) =
            ConfigEditorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HANDLE, handle)
                    putString(ARG_PKG, pkg)
                }
            }
    }
}
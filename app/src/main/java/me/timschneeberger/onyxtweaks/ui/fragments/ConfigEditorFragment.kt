package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.IMMKVAccessService
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.activities.ConfigEditorActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.services.unmarshallFromPipe
import me.timschneeberger.onyxtweaks.ui.utils.showAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.NONE)
class ConfigEditorFragment : SettingsBaseFragment<ConfigEditorActivity>() {
    private var handle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        handle = requireArguments().getString(ARG_HANDLE)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        parentActivity?.mmkvService?.close(handle)
        super.onDestroy()
    }

    override fun onConfigurePreferences() {
        PreferenceCategory(requireContext()).apply {
            title = "Key-value pairs"
            isIconSpaceReserved = false
        }.let { root ->
            preferenceScreen.addPreference(root)

            root.addPreference(
                Preference(requireContext()).apply {
                    setIcon(R.drawable.ic_twotone_info_24dp)
                    summary =
                        "MMKV stores key-value pairs without data type information. Data type information for some keys are hard-coded in this app. Others are guessed based on the data and may be incorrect."
                    isIconSpaceReserved = false
                    isSelectable = false
                }
            )

            val service = parentActivity?.mmkvService
            if(handle == null || service == null) {
                return
            }

            service.allKeys(handle).also { strings -> Log.i("Keys: ${strings.joinToString()}") }.forEach { key ->
                root.addPreference(
                    Preference(requireContext()).apply {
                        title = key
                        summary = service.guessTypeAndConvert(key, true).toString()
                        isIconSpaceReserved = false
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            requireContext().showAlert(
                                "Key: $key",
                                "Value: ${service.guessTypeAndConvert(key, false)}",
                            )
                            true
                        }
                    }
                )
            }
        }
    }

    fun IMMKVAccessService.getString(handle: String?, key: String): String? {
        getLargeString(handle, key).also { fd ->
            fd.unmarshallFromPipe().let {
                val string = it.readString()
                it.recycle()
                return string
            }
        }
    }

    fun IMMKVAccessService.getStringSet(handle: String?, key: String): List<String> {
        getLargeStringSet(handle, key).also { fd ->
            fd.unmarshallFromPipe().let {
                val list = mutableListOf<String>()
                it.readStringList(list)
                it.recycle()
                return list
            }
        }
    }

    fun IMMKVAccessService.guessTypeAndConvert(key: String, truncatedPreview: Boolean): Any? {
        var size = getValueActualSize(handle, key)
        val strVal by lazy { if (truncatedPreview) getTruncatedString(handle, key, 200) else getString(handle, key) }

        return when {
            size == 0 -> null // empty; unknown type
            size <= 4 -> getInt(handle, key) // warning: could also be a float or boolean
            size <= 8 -> getLong(handle, key) // warning: could also be a double
            // If it's String-like with control characters, it's likely a set because each
            // string entry has a int32 length field which would may be parsed as non-printable chars
            strVal?.any { ch -> ch.isISOControl() } == true -> if(truncatedPreview) getTruncatedStringSet(handle, key, 200) else getStringSet(handle, key)
            // Otherwise, it's likely a string
            else -> strVal
        }
    }

    companion object {
        private const val ARG_HANDLE = "handle"

        fun newInstance(handle: String) =
            ConfigEditorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HANDLE, handle)
                }
            }
    }
}
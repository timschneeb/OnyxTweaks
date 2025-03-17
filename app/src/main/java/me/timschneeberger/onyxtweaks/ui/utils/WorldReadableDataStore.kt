package me.timschneeberger.onyxtweaks.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceDataStore
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File

class WorldReadableDataStore(private val context: Context, private val group: PreferenceGroups) : PreferenceDataStore(), SharedPreferences.OnSharedPreferenceChangeListener {
    var onDataStoreModified: ((key: String) -> Unit)? = null

    @Suppress("DEPRECATION")
    @SuppressLint("WorldReadableFiles")
    val prefs: SharedPreferences = context.getSharedPreferences(group.prefName, Context.MODE_WORLD_READABLE).also { preferences ->
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    @SuppressLint("SetWorldReadable")
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        /*
            For some reason, LSPosed's new XSharedPreferences doesn't set the file to world readable
            despite the MODE_WORLD_READABLE flag being set.
            This is a workaround to set the file readable after every change.

            Important: ALWAYS use synchronous commits to save preferences!
                       Otherwise, when using the async apply method,
                       onSharedPreferenceChanged will be called before the changes are saved to disk.
         */
        HiddenApiBypass
            .invoke(Context::class.java, context, "getSharedPreferencesPath", group.prefName)
            .cast<File>()
            ?.also { it.setReadable(true, false) }
            ?: Log.e("Failed to set preferences file readable! getSharedPreferencesPath call failed (group: ${group.prefName})")
    }

    private fun SharedPreferences.commit(key: String, block: SharedPreferences.Editor.() -> Unit) =
        prefs.edit().apply(block).commit().let { onDataStoreModified?.invoke(key); Unit }

    override fun putString(key: String, value: String?) = prefs.commit(key) { putString(key, value) }
    override fun putStringSet(key: String, values: Set<String?>?) = prefs.commit(key) { putStringSet(key, values) }
    override fun putInt(key: String, value: Int) = prefs.commit(key) { putInt(key, value) }
    override fun putLong(key: String, value: Long) = prefs.commit(key) { putLong(key, value) }
    override fun putFloat(key: String, value: Float) = prefs.commit(key) { putFloat(key, value) }
    override fun putBoolean(key: String, value: Boolean) = prefs.commit(key) { putBoolean(key, value) }

    override fun getString(key: String, defValue: String?) = prefs.getString(key, defValue)
    override fun getStringSet(key: String, defValues: Set<String?>?) = prefs.getStringSet(key, defValues)
    override fun getInt(key: String, defValue: Int) = prefs.getInt(key, defValue)
    override fun getLong(key: String, defValue: Long) = prefs.getLong(key, defValue)
    override fun getFloat(key: String, defValue: Float) = prefs.getFloat(key, defValue)
    override fun getBoolean(key: String, defValue: Boolean) = prefs.getBoolean(key, defValue)
}
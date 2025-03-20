package me.timschneeberger.onyxtweaks.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import com.github.kyuubiran.ezxhelper.EzXHelper.moduleRes
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.XSharedPreferences
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.preferences.WorldReadableDataStore
import kotlin.reflect.KClass

enum class PreferenceGroups(@XmlRes val xmlRes: Int, val prefName: String) {
    ROOT(R.xml.app_preferences, "app_preferences"),
    LAUNCHER(R.xml.app_launcher_preferences, "app_launcher_preferences"),
    STATUS_BAR(R.xml.app_status_bar_preferences, "app_status_bar_preferences"),
    QS(R.xml.app_qs_preferences, "app_qs_preferences"),
    RECENTS(R.xml.app_recents_preferences, "app_recents_preferences"),
    EINK(R.xml.app_eink_opt_preferences, "app_eink_opt_preferences"),
    MISC(R.xml.app_misc_preferences, "app_misc_preferences"),
    ABOUT(R.xml.app_about_preferences, "app_about_preferences"),
    NONE(R.xml.app_empty_preferences, "")
}

/**
 * @remarks This class is used to access preferences with read-only without an app context using XSharedPreferences.
 *          Only for use in hooked apps.
 */
class XPreferences(group: PreferenceGroups) : BasePreferences() {
    override val isReadOnly = true
    override val resources get() = moduleRes

    override val prefs: SharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID, group.prefName).also { it ->
        if(!it.file.exists())
            Log.ix("'${group.prefName}' not yet created. Using defaults.")
        else if (!it.file.canRead())
            Log.ex("CRITICAL: Preferences file '${group.prefName}' not readable")

        try {
            @Suppress("DEPRECATION")
            it.registerOnSharedPreferenceChangeListener(this)
        }
        catch (e: UnsupportedOperationException) {
            Log.wx("Failed to register XPreference change listener", e)
        }
    }
}

/**
 * @remarks This class is used to access preferences with read-write capabilities using SharedPreferences.
 *          Do not use in hooked apps.
 */
class Preferences(private val context: Context, group: PreferenceGroups) : BasePreferences() {
    private val dataStore = WorldReadableDataStore(context, group).also {
        it.prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override val isReadOnly = false
    override val resources: Resources get() = context.resources
    override val prefs: SharedPreferences = dataStore.prefs
}

abstract class BasePreferences() : SharedPreferences.OnSharedPreferenceChangeListener {
    var onPreferencesChanged: ((String?) -> Unit)? = null

    private val defaultCache: HashMap<String, Any> = hashMapOf()

    protected abstract val prefs: SharedPreferences
    protected abstract val resources: Resources
    protected abstract val isReadOnly: Boolean

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        loadEverything(key)
    }

    private fun loadEverything(vararg key: String?) {
        key.filterNotNull().forEach {
            onPreferencesChanged?.invoke(it)
        }
    }

    fun getStringAsInt(@StringRes nameRes: Int, default: Int = 0): Int {
        return get(nameRes, default.toString(), String::class).toIntOrNull()
            ?: getDefault(nameRes, String::class).toInt()
    }

    /**
     * @remarks This function takes a StringRes pointing to a preference key.
     *          There MUST be a default value with the same key name in defaults.xml,
     *          otherwise an exception will be thrown when loading the default value.
     *
     * @return Returns the current value of the preference or the default value if none is set
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(@StringRes nameRes: Int, default: T? = null, type: KClass<T>): T {
        val key = resources.getString(nameRes)
        val defValue = default ?: getDefault(nameRes, type)

        return when(type) {
            Boolean::class -> prefs.getBoolean(key, defValue as Boolean) as T
            String::class -> prefs.getString(key, defValue as String) as T
            Int::class -> prefs.getInt(key, defValue as Int) as T
            Long::class -> prefs.getLong(key, defValue as Long) as T
            Float::class -> prefs.getFloat(key, defValue as Float) as T
            Set::class -> prefs.getStringSet(key, defValue as Set<String>) as T
            else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
        }.also {
            // CrashlyticsImpl.setCustomKey("${namespace}_$key", it.toString())
        }
    }

    inline fun <reified T : Any> get(@StringRes nameRes: Int): T {
        return get<T>(nameRes, null, T::class)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getDefault(@StringRes nameRes: Int, type: KClass<T>): T {
        val key = resources.getString(nameRes)
        return if(defaultCache.containsKey(key)) {
            defaultCache[key] as T
        }
        else {
            @SuppressLint("DiscouragedApi")
            val defaultRes = resources.getIdentifier(
                "default_$key",
                when(type)
                {
                    Boolean::class -> "bool"
                    String::class -> "string"
                    Int::class -> "integer"
                    Long::class -> "integer"
                    Float::class -> "integer"
                    Set::class -> "array"
                    else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
                },
                BuildConfig.APPLICATION_ID
            )

            if(defaultRes == 0) {
                throw IllegalStateException("Preference key '$key' has no default set")
            }

            (when(type) {
                Boolean::class -> resources.getBoolean(defaultRes)
                String::class -> resources.getString(defaultRes)
                Int::class -> resources.getInteger(defaultRes)
                Long::class -> resources.getInteger(defaultRes).toLong()
                Float::class -> resources.getInteger(defaultRes).toFloat()
                Set::class -> resources.getStringArray(defaultRes).toSet()
                else -> throw IllegalArgumentException("Unknown type")
            } as T).also {
                defaultCache[key] = it as Any
            }
        }
    }

    inline fun <reified T : Any> getDefault(@StringRes nameRes: Int): T {
        return getDefault(nameRes, T::class)
    }

    @SuppressLint("ApplySharedPref")
    fun <T : Any> reset(@StringRes nameRes: Int, type: KClass<T>) {
        set(nameRes, getDefault(nameRes, type), type)
    }

    inline fun <reified T : Any> reset(@StringRes nameRes: Int) {
        return reset(nameRes, T::class)
    }

    @SuppressLint("ApplySharedPref")
    fun <T : Any> set(@StringRes nameRes: Int, value: T, type: KClass<T>) {
        if(isReadOnly) {
            throw IllegalStateException("This preferences implementation is read-only")
        }

        val key = resources.getString(nameRes)
        val edit = prefs.edit()
        // CrashlyticsImpl.setCustomKey("${namespace}_$key", value.toString())

        when(type) {
            Boolean::class -> edit.putBoolean(key, value as Boolean)
            String::class -> edit.putString(key, value as String)
            Int::class -> edit.putInt(key, value as Int)
            Long::class -> edit.putLong(key, value as Long)
            Float::class -> edit.putFloat(key, value as Float)
            else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
        }.run(SharedPreferences.Editor::commit)
    }

    inline fun <reified T : Any> set(@StringRes nameRes: Int, value: T) {
        set(nameRes, value, T::class)
    }
}